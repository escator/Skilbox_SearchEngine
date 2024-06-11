package searchengine.repository;

import java.util.HashSet;

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
