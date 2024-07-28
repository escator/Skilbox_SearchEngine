package searchengine.services;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

/**
 * <p>Функционал класса заключается в хранении управляющих флагов и мониторинге работы пулов потоков осуществляющих индексиоование сайта.
 * При создании отдельного потока в ThreadIndexingManager
 * он регистрируется в списке потоков, которые будут
 * индексировать сайт, а после выполнения работы поток
 * исключается из списка потоков.
 * Синхронизированные методы regIndexer и unregIndexer реализуют функционал регистрации и отмены регистрации индексаторов.</p>
 */
@Slf4j
public class RunIndexMonitor {
    private static HashSet<ThreadIndexingManager> indexers = new HashSet<>();
    private static boolean stopIndexing = false;
    private static boolean isIndexingRunning = false;
    public static synchronized void regIndexer(ThreadIndexingManager indexer){
        indexers.add(indexer);
        switchIndexMonitor();
        log.info("Registering indexer {}", indexer.getPageDto().getUrl());
    }

    public static synchronized void unregIndexer(ThreadIndexingManager indexer){
        indexers.remove(indexer);
        switchIndexMonitor();
        log.info("Unregistering indexer {}", indexer.getPageDto().getUrl());
    }

    private static void switchIndexMonitor(){
        if(indexers.size() > 0) {
            isIndexingRunning = true;
        } else {
            isIndexingRunning  = false;
            stopIndexing  = false;
        }
        log.info("isIndexingRunning is {}", isIndexingRunning);
    }

    public static void setStopIndexing(boolean stop) {
        stopIndexing = stop;
    }

    public static boolean isStopIndexing()  {
        return stopIndexing;
    }

    public static void setIndexingRunning(boolean running)  {
        isIndexingRunning = running;
    }

    public static boolean isIndexingRunning()  {
        return isIndexingRunning;
    }
}
