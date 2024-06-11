package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import searchengine.Application;
import searchengine.dto.index.HtmlParseResponse;
import searchengine.dto.index.PageDto;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.LinkStorage;
import searchengine.repository.PageRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class PageScannerService extends RecursiveAction {

    // адрес сканируемой страницы
    private String url;
    private String rootUrl;
    private Site site;
    private PageRepository pageRepository;
    private HashSet<String> visitedLinks;

    // конструкторы
    public PageScannerService(PageDto pageDto)  {
        if (pageDto.getRootUrl().endsWith("/")) {
            this.rootUrl = pageDto.getUrl().substring(0, pageDto.getUrl().length() - 1);
        } else {
            this.rootUrl  = pageDto.getRootUrl();
        }
        this.url = pageDto.getUrl();
        this.site  = pageDto.getSite();
        this.pageRepository = pageDto.getPageRepository();
    }

    // end конструкторы
    @Override
    protected void compute() {
        if (RunIndexMonitor.isStopIndexing()) {
            return;
        }
        if (LinkStorage.containsLink(url)) {
            return;
        }
        // Выдерживаем паузу в 200 - 300мс перед началом загрузки страницы
        // рандомность для снижения шанса блокировки
        pause(200, 500);
        LinkStorage.addLink(url);

        // Список ветвей рекурсии для каждой ссылки
        List<PageScannerService> tasks = new ArrayList<>();

        HtmlParseService htmlParseService  = new HtmlParseService(url, rootUrl);
        // Получаем множество всех ссылок на странице без дублей
        Set<String> linksOnPageList = htmlParseService.getAllLinksOnPage();


        // Получаем doc и статус
        HtmlParseResponse htmlParseResponse = htmlParseService.parse();
        if (!isVisitedLinks(getShortUrl(url))) {
            savePageToRepository(url, htmlParseResponse);
        }

        // Создаем ветку рекурсии для каждой ссылки на странице
        for (String link : linksOnPageList) {
            PageDto pageDto = new PageDto(link, rootUrl, site, pageRepository);
            PageScannerService task = new PageScannerService(pageDto);
            tasks.add(task);
            task.fork();
        }
    }

    private void savePageToRepository(String url, HtmlParseResponse htmlParseResponse)  {
        Page page = new Page();
        page.setPath(getShortUrl(url));
        page.setCode(htmlParseResponse.getStatus());
        page.setSite(site);
        page.setContent(htmlParseResponse.getDocument().toString());
        synchronized (pageRepository) {
            pageRepository.save(page);
        }
    }

    private String getShortUrl(String url)   {
        if (url.endsWith("/")) {
            url.substring(0, url.length() - 1);
        }
        if (url == rootUrl) {
            return "/";
        }
        return url.substring(rootUrl.length());
    }

    private boolean isVisitedLinks(String url) {
        Page page = new Page();
        page.setPath(url);
        Example<Page> example  = Example.of(page);
        return pageRepository.exists(example);
    }

    private void pause(int min, int max)  {
        int duration = min + (int)(Math.random() * (max - min));
        try {
            log.info(Thread.currentThread().getName() + " sleeping for " + duration + " ms");
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            log.info("InterruptedException while sleep", e);
            throw new RuntimeException(e);
        }
    }

}
