package searchengine.repository;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
@Slf4j
public class LinkStorage {
    private static ConcurrentSkipListSet<String> links = new ConcurrentSkipListSet<>();

    public static synchronized void addLink(String link){
        links.add(link);
    }

    public static void addAll(Set<String> linksSet) {
        links.addAll(linksSet);
    }

    public static Set<String> getLinks(){
        return links;
    }

    public static synchronized boolean containsLink(String link) {
        return links.contains(link);
    }

    public static void removeAll() {
        links.clear();
    }
}
