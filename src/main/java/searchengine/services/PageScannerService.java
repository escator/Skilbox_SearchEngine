package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import searchengine.dto.index.HtmlParseResponse;
import searchengine.dto.index.PageDto;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class PageScannerService extends RecursiveAction {

    // адрес сканируемой страницы
    private String url;
    private String rootUrl;
    private Site site;
    private PageRepository pageRepository;

    // конструкторы
    public PageScannerService(PageDto pageDto)  {
        this.rootUrl  = pageDto.getRootUrl();
        this.url = pageDto.getUrl();
        this.site  = pageDto.getSite();
        this.pageRepository = pageDto.getPageRepository();
    }

    public PageScannerService(String rootUrl, String url) {
        this.rootUrl  = rootUrl;
        this.url = url;
    }
    // end конструкторы

    @Override
    protected void compute() {

        if (isVisitedLinks()) {
            return;
        }

        // Выдерживаем паузу в 200 - 300мс перед началом загрузки страницы
        // рандомность для снижения шанса блокировки
        pause();

        // Список ветвей рекурсии для каждой ссылки
        List<PageScannerService> tasks = new ArrayList<>();

        HtmlParseService htmlParseService  = new HtmlParseService(url, rootUrl);

        // Получаем doc и статус
        HtmlParseResponse htmlParseResponse = htmlParseService.parse();


        Page page = new Page();
        page.setPath(url);
        page.setCode(htmlParseResponse.getStatus().value());
        page.setSite(site);
        page.setContent(htmlParseResponse.getDocument().toString());
        synchronized (pageRepository) {
            pageRepository.save(page);
        }

        // Получаем множество всех ссылок на странице без дублей
        List<String> linksOnPageList = htmlParseService.getAllLinksOnPage();


        // Создаем ветку рекурсии для каждой ссылки на странице
        for (String link : linksOnPageList) {
            PageDto pageDto = new PageDto(link, rootUrl, site, pageRepository);
            PageScannerService task = new PageScannerService(pageDto);
            tasks.add(task);
            task.fork();
        }

    }


    private boolean isVisitedLinks() {
        Page page = new Page();
        page.setPath(url);
        Example<Page> example  = Example.of(page);
        return pageRepository.exists(example);
    }

    private void pause()  {
        int duration = 150 + (int)(Math.random() * 150);
        try {
            log.info(Thread.currentThread().getName() + " sleeping for " + duration + " ms");
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            log.info("InterruptedException while sleep", e);
            throw new RuntimeException(e);
        }
    }

}
