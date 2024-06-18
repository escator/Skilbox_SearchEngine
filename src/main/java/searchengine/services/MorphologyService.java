package searchengine.services;

import searchengine.dto.index.PageDto;
import searchengine.model.Site;

import java.util.HashMap;

public interface MorphologyService {
    HashMap<String, Integer> getLemmas(String text);

    void process(IndexService indexService, Site site);
}
