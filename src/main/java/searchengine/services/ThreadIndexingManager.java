package searchengine.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import searchengine.dto.index.PageDto;
import searchengine.response.PageScannerResponse;
import searchengine.model.IndexingStatus;
import searchengine.repository.LinkStorage;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;

/**
 * Класс, реализующий индексирование страниц каждого сайта
 * из отдельного потока.
 */
@Slf4j
public class ThreadIndexingManager implements Runnable {
    @Getter
    PageDto pageDto;
    private final IndexService indexService;
    private final SiteService siteService;
    public ThreadIndexingManager(PageDto pageDto, IndexService indexService) {
        this.pageDto = pageDto;
        this.indexService = indexService;
        this.siteService = indexService.getSiteService();
    }
    @Override
    public void run() {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        PageScannerResponse response = forkJoinPool.invoke(new PageScannerService(pageDto, indexService));
        RunIndexMonitor.regIndexer(this);

        // Ожидаем когда отработают весь пул
        while (forkJoinPool.getQueuedSubmissionCount() > 0 ||
                forkJoinPool.getActiveThreadCount() > 0) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (RunIndexMonitor.isStopIndexing()) {
            siteService.updateStatusOnSite(pageDto.getSite(), IndexingStatus.FAILED);
            siteService.updateLastErrorOnSite(pageDto.getSite(), "Индексация остановлена пользователем");
        } else if (response.getStatus() == PageScannerResponse.status.ERROR) {
            siteService.updateStatusOnSite(pageDto.getSite(), IndexingStatus.FAILED);
            siteService.updateLastErrorOnSite(pageDto.getSite(), response.getMessage());
        } else  {
            // Обрабатываем леммы
            try {
                MorphologyService morphologyService = new MorphologyServiceImpl(indexService);
                morphologyService.processSite(indexService, pageDto.getSite());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            siteService.updateStatusOnSite(pageDto.getSite(), IndexingStatus.INDEXED);
        }
        RunIndexMonitor.unregIndexer(this);
        LinkStorage.clear();
    }
}
