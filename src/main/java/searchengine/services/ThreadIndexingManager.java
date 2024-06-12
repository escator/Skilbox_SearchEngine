package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import searchengine.dto.index.PageDto;
import searchengine.dto.index.PageScannerResponse;
import searchengine.repository.LinkStorage;

import java.util.concurrent.ForkJoinPool;

/**
 * Класс, реализующий индексирование страниц каждого сайта
 * из отдельного потока.
 */
@Slf4j
public class ThreadIndexingManager implements Runnable {
    PageDto pageDto;
    public ThreadIndexingManager(PageDto pageDto){
        this.pageDto = pageDto;
    }
    @Override
    public void run() {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        PageScannerResponse response = forkJoinPool.invoke(new PageScannerService(pageDto));
        RunIndexMonitor.regIndexer(this);

        // Ожидаем когда отработают весь пулл
        while (forkJoinPool.getQueuedSubmissionCount() > 0 ||
                forkJoinPool.getActiveThreadCount() > 0) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        LinkStorage.clear();
        RunIndexMonitor.unregIndexer(this);
        log.info("ThreadIndexingManager finished. Response: {}", response.getStatus());
    }
}
