package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import searchengine.dto.index.PageDto;
import searchengine.dto.index.PageScannerResponse;
import searchengine.model.IndexingStatus;
import searchengine.repository.LinkStorage;

import java.util.concurrent.ForkJoinPool;

/**
 * Класс, реализующий индексирование страниц каждого сайта
 * из отдельного потока.
 */
@Slf4j
public class ThreadIndexingManager implements Runnable {
    PageDto pageDto;
    IndexServiceImpl service;
    public ThreadIndexingManager(PageDto pageDto, IndexServiceImpl service){
        this.pageDto = pageDto;
        this.service = service;
    }
    @Override
    public void run() {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        PageScannerResponse response = forkJoinPool.invoke(new PageScannerService(pageDto, service.getPageRepository()));
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
            service.updateStatus(pageDto.getSite(), IndexingStatus.FAILED);
            service.updateLastError(pageDto.getSite(), "Indexing was stopped");
        } else  {
            service.updateStatus(pageDto.getSite(), IndexingStatus.INDEXED);
        }
        RunIndexMonitor.unregIndexer(this);
        LinkStorage.clear();
    }
}
