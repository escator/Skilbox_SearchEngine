package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.index.SiteDto;
import searchengine.response.IndexingResponse;
import searchengine.response.StatisticsResponse;
import searchengine.model.Site;
import searchengine.response.SearchResponse;
import searchengine.services.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    private final IndexService indexService;
    private final SiteService siteService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService,
                         IndexService indexService,
                         SearchService searchService,
                         SiteService siteService) {
        this.statisticsService = statisticsService;
        this.indexService = indexService;
        this.searchService  = searchService;
        this.siteService = siteService;
    }

////////////////TEST METHODS /////////////////////
//TODO удалить все тестовые методы перед презентацией проекта

    @GetMapping("/delete")
    public ResponseEntity<IndexingResponse> delete() {
        //TODO удалить метод после тестирования
        log.info("delete test data");
        List<Site> list = siteService.findAllSites();
        log.info("delete " + list.get(0).toString());
        siteService.deleteSite(list.get(0));
        return null;
    }

    @GetMapping("/getLinks")
    public void getLinks(@RequestParam String url) {
        log.info("Controller: get links: " + url);
        indexService.indexingSite(new SiteDto(url, "Test site"));
    }

    @PostMapping("/find")
    public void find(@RequestBody SiteDto siteDto) {
        Site res = siteService.findSite(null, siteDto.getName(), siteDto.getUrl());
        log.info("Controller: find url:  " + siteDto.getUrl() + " is " + res.toString());
    }

    @PostMapping("/delete")
    public void delete(@RequestBody SiteDto siteDto) {
        siteService.deleteSite(siteService.findSite(null, siteDto.getName(), siteDto.getUrl()));
        log.info("Controller: delete url:  " + siteDto.getUrl());
    }

    @GetMapping("/test")
    public void getList() {
        log.info("Controller: test");
        //morphologyService.test();
    }
/////////END TEST METHODS //////////////

    // ОСНОВНОЕ API ////////////
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        log.info("Controller: start indexing");
        if (RunIndexMonitor.isIndexingRunning()) {
            return new ResponseEntity<IndexingResponse>(
                    new IndexingResponse(false, "Индексация уже запущена"),
                    HttpStatus.FORBIDDEN);
        }
        IndexingResponse res = indexService.indexingAllSites();

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
        log.info("Controller: index page {}", siteDto.getUrl());
        IndexingResponse response = indexService.indexingPage(siteDto);
        return new ResponseEntity<IndexingResponse>(response, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required  = false) Integer limit,
            @RequestParam(required = false) String site) {
        SearchResponse response = searchService.search(query, offset, limit, site);
        return null;
    }
}
