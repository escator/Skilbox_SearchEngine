package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import searchengine.dto.index.PageDto;
import searchengine.dto.index.PageScannerResponse;
import searchengine.repository.LinkStorage;
import searchengine.repository.PageRepository;

import java.util.concurrent.ForkJoinPool;

/**
 * Класс, реализующий индексирование страниц каждого сайта
 * из отдельного потока.
 */
@Slf4j
public class ThreadIndexingManager implements Runnable {
    PageDto pageDto;
    PageRepository repository;
    public ThreadIndexingManager(PageDto pageDto, PageRepository repository){
        this.pageDto = pageDto;
        this.repository = repository;
    }
    @Override
    public void run() {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        PageScannerResponse response = forkJoinPool.invoke(new PageScannerService(pageDto, repository));
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
        if (RunIndexMonitor.isStopIndexing()) {
            log.info("ThreadIndexingManager was stopped");
        }
        RunIndexMonitor.unregIndexer(this);
    }
}
