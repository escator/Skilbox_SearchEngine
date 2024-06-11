package searchengine.services;

import searchengine.dto.index.PageDto;
import searchengine.repository.LinkStorage;

import java.util.concurrent.ForkJoinPool;

public class ThreadIndexingStarter implements Runnable {
    PageDto pageDto;
    public ThreadIndexingStarter(PageDto pageDto){
        this.pageDto = pageDto;
    }
    @Override
    public void run() {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.invoke(new PageScannerService(pageDto));
        while (forkJoinPool.getQueuedSubmissionCount() > 0 ||
                forkJoinPool.getActiveThreadCount() > 0) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        LinkStorage.clear();
//        if (Application.isStopIndexing()) {
//            updateStatus(site, IndexingStatus.FAILED);
//        } else  {
//            updateStatus(site, IndexingStatus.INDEXED);
//        }

    }
}
