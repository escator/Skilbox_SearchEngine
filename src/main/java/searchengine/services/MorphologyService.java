package searchengine.services;

import searchengine.dto.index.PageDto;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.HashMap;
import java.util.List;

public interface MorphologyService {
    HashMap<String, Integer> getLemmas(String text);
    public void processOnePage(IndexService indexService, Page page);
    public void processSite(IndexService indexService, Site site);
    //void process(IndexService indexService, Site site);
    public List<String> getWords(String text);
    public boolean isNotWord(List<String> words);
}
