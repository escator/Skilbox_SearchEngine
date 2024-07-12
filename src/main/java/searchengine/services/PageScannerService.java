package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import searchengine.response.HtmlParseResponse;
import searchengine.dto.index.PageDto;
import searchengine.response.PageScannerResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.LinkStorage;
import searchengine.util.LinkToolsBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class PageScannerService extends RecursiveTask<PageScannerResponse> {

    // адрес сканируемой страницы
    private final String url;
    private final String rootUrl;
    private final Site site;
    private final IndexService indexService;

    // конструкторы
    public PageScannerService(PageDto pageDto, IndexService service)  {
        this.rootUrl = LinkToolsBox.normalizeRootUrl(pageDto.getRootUrl());
        this.url = pageDto.getUrl();
        this.site  = pageDto.getSite();
        this.indexService = service;
    }

    // end конструкторы
    @Override
    protected PageScannerResponse compute() {
        if (RunIndexMonitor.isStopIndexing()) {
            return PageScannerResponse.getStopResponse();
        }
        if (LinkStorage.containsLink(url)) {
            return new PageScannerResponse(
                    PageScannerResponse.status.DOUBLE_LINK,
                    "Ссылка уже добавлена");
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
        // Если не удалось получить главную страницу сайта. Возвращаем ошибку
        if (url.equals(rootUrl) && htmlParseResponse.getStatus() != 200)  {
            return PageScannerResponse.getErrorResponse();
        }

        //TODO вставить обработку лемм сюда

        //TODO принять решение по нужности данной проверки в БД.
        if (!indexService.isVisitedLinks(LinkToolsBox.getShortUrl(url, rootUrl))) {
            savePageToRepository(url, htmlParseResponse);
        }

        // Создаем ветку рекурсии для каждой ссылки на странице
        for (String link : linksOnPageList) {
            PageDto pageDto = new PageDto(link, rootUrl, site);
            PageScannerService task = new PageScannerService(pageDto, indexService);
            task.fork();
        }
        return RunIndexMonitor.isStopIndexing() ? PageScannerResponse.getStopResponse()
                : PageScannerResponse.getOKResponse();
    }

    private void savePageToRepository(String url, HtmlParseResponse htmlParseResponse)  {
        Page page = new Page();
        page.setPath(LinkToolsBox.getShortUrl(url, rootUrl));
        page.setCode(htmlParseResponse.getStatus());
        page.setSite(site);
        page.setContent(htmlParseResponse.getDocument().toString());
        indexService.savePage(page);
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
