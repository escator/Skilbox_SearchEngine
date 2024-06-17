package searchengine.services;

import java.util.HashMap;

public interface MorphologyService {
    HashMap<String, Integer> getLemmas(String text);

}
