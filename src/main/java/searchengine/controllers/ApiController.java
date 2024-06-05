package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SiteDto;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Site;
import searchengine.services.IndexServiceImpl;
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

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }


////////////////TEST METHODS /////////////////////
//TODO удалить все тестовые методы перед презентацией проекта

@GetMapping("/delete")
    public ResponseEntity<IndexingResponse> delete()  {
        //TODO удалить метод после тестирования
        log.info("delete test data");
        List<Site> list = indexService.findAll();
        log.info("delete " + list.get(0).toString());
        indexService.delete(list.get(0));
        return null;
    }
    @GetMapping("/setTestData")
    public ResponseEntity<IndexingResponse> setTestData()  {
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
/////////END TEST METHODS //////////////


    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        //TODO implement
        // Метод запускает полную индексацию всех сайтов или полную
        // переиндексацию, если они уже проиндексированы.
        // Если в настоящий момент индексация или переиндексация уже
        // запущена, метод возвращает соответствующее сообщение об ошибке.
        log.info("Controller: start indexing");
        indexService.indexingAll();

        return null;
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        //TODO implement
        // Метод останавливает текущий процесс индексации (переиндексации).
        // Если в настоящий момент индексация или переиндексация не происходит,
        // метод возвращает соответствующее сообщение об ошибке.
        return null;
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage()  {
        //TODO implement
        // Метод добавляет в индекс или обновляет отдельную страницу, адрес
        // которой передан в параметре. Возвращает статус индекса.
        // Если адрес страницы передан неверно, метод должен вернуть
        // соответствующую ошибку.

        return null;
    }
}
