package searchengine.repository;

import java.util.HashSet;

public class LinkStorage {
    private static HashSet<String> linksHash = new HashSet<>();

    public static synchronized void addLink(String link){
        linksHash.add(link);
    }

    public static synchronized boolean containsLink(String link) {
        return linksHash.contains(link);
    }

    public static void clear() {
        linksHash.clear();
    }
}
