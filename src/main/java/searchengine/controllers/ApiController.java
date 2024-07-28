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
        return new ResponseEntity(response, HttpStatus.OK);
    }
}
