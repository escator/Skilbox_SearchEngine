package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.index.SiteDto;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Site;
import searchengine.services.IndexServiceImpl;
import searchengine.services.RunIndexMonitor;
import searchengine.services.StatisticsService;
import searchengine.util.TestDataLoader;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    //TODO расссмотреть имплементацию по интерфейсу
    private final IndexServiceImpl indexService;

    //TODO удалить метод после тестирования
    @Autowired
    private TestDataLoader testDataLoader;

    public ApiController(StatisticsService statisticsService, IndexServiceImpl indexService) {
        this.statisticsService = statisticsService;
        this.indexService = indexService;
    }

////////////////TEST METHODS /////////////////////
//TODO удалить все тестовые методы перед презентацией проекта

    @GetMapping("/delete")
    public ResponseEntity<IndexingResponse> delete() {
        //TODO удалить метод после тестирования
        log.info("delete test data");
        List<Site> list = indexService.findAll();
        log.info("delete " + list.get(0).toString());
        indexService.deleteSite(list.get(0));
        return null;
    }

    @GetMapping("/setTestData")
    public ResponseEntity<IndexingResponse> setTestData() {
        //TODO удалить метод после тестирования
        log.info("Controller: set test data");
        testDataLoader.loadTestSites();
        return null;
    }

    @GetMapping("/getLinks")
    public void getLinks(@RequestParam String url) {
        log.info("Controller: get links: " + url);
        indexService.indexingSite(new SiteDto(url, "Test site"));
    }

    @PostMapping("/validateUrl")
    public void validateUrl(@RequestBody SiteDto site) {
        boolean res = indexService.isValidSite(site);
        log.info("Controller: validate url: " + site.getUrl() + " is " + res);
    }

    @PostMapping("/find")
    public void find(@RequestBody SiteDto siteDto) {
        Site res = indexService.findSite(null, siteDto.getName(), siteDto.getUrl());
        log.info("Controller: find url:  " + siteDto.getUrl() + " is " + res.toString());
    }

    @PostMapping("/delete")
    public void delete(@RequestBody SiteDto siteDto) {
        indexService.deleteSite(indexService.findSite(null, siteDto.getName(), siteDto.getUrl()));
        log.info("Controller: delete url:  " + siteDto.getUrl());
    }

    @GetMapping("/test")
    public void getList() {
        log.info("Controller: test");
        indexService.test();
    }
/////////END TEST METHODS //////////////

    // ОСНОВНОЕ API ////////////
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        //TODO implement
        // Метод запускает полную индексацию всех сайтов или полную
        // переиндексацию, если они уже проиндексированы.
        // Если в настоящий момент индексация или переиндексация уже
        // запущена, метод возвращает соответствующее сообщение об ошибке.
        log.info("Controller: start indexing");
        if (RunIndexMonitor.isIndexingRunning()) {
            return new ResponseEntity<IndexingResponse>(
                    new IndexingResponse(false, "Индексация уже запущена"),
                    HttpStatus.FORBIDDEN);
        }
        IndexingResponse res = indexService.indexingAll();

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        if (!RunIndexMonitor.isIndexingRunning())  {
            return new ResponseEntity<IndexingResponse>(
                    new IndexingResponse(false, "Индексация не запущена"),
                    HttpStatus.FORBIDDEN);
        } else {
            RunIndexMonitor.setStopIndexing(true);
            return new ResponseEntity<IndexingResponse>(
                    new IndexingResponse(true,  null),
                    HttpStatus.OK);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(SiteDto siteDto)  {
        //TODO implement
        // Метод добавляет в индекс или обновляет отдельную страницу, адрес
        // которой передан в параметре. Возвращает статус индекса.
        // Если адрес страницы передан неверно, метод должен вернуть
        // соответствующую ошибку.
        log.info("Controller: index page {}", siteDto.getUrl());
        IndexingResponse response = indexService.indexingPage(siteDto);
        return new ResponseEntity<IndexingResponse>(response, HttpStatus.OK);
    }
}
