package searchengine.repository;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

@Slf4j
public class LinkStorage {
    private static HashSet<Integer> linksHash = new HashSet<>();

    public static synchronized void addLink(String link){
        linksHash.add(link.hashCode());
    }

    public static synchronized boolean containsLink(String link) {
        return linksHash.contains(link.hashCode());
    }

    public static void clear() {
        linksHash.clear();
    }
}
