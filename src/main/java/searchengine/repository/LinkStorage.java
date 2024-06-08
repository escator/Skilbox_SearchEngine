package searchengine.repository;

import java.util.HashSet;
import java.util.Set;

public class LinkStorage {
    private static HashSet<String> links = new HashSet<>();

    public static synchronized void addLink(String link){
        links.add(link);
    }

    public static synchronized void addAll(Set<String> linksSet) {
        links.addAll(linksSet);
    }

    public static synchronized Set<String> getLinks(){
        return links;
    }

    public static synchronized boolean containsLink(String link) {
        return links.contains(link);
    }

    public static void removeAll() {
        links.clear();
    }
}
