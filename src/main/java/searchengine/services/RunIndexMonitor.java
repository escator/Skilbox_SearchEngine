package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import searchengine.Application;

import java.util.HashSet;
@Slf4j
public class RunIndexMonitor {
    private static HashSet<ThreadIndexingStarter> indexers = new HashSet<ThreadIndexingStarter>();
    private static boolean stopIndexing = false;
    private static boolean isIndexingRunning = false;
    public static synchronized void regIndexer(ThreadIndexingStarter indexer){
        indexers.add(indexer);
        switchIndexMonitor();
        log.info("Registering indexer {}", indexer);
    }

    public static synchronized void unregIndexer(ThreadIndexingStarter indexer){
        indexers.remove(indexer);
        switchIndexMonitor();
        log.info("Unregistering indexer {}", indexer);
    }

    private static void switchIndexMonitor(){
        if(indexers.size()>0){
            isIndexingRunning = true;
        } else {
            isIndexingRunning  = false;
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
