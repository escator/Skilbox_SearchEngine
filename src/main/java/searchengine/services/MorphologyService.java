package searchengine.services;

import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.HashMap;
import java.util.List;

public interface MorphologyService {
    HashMap<String, Integer> getLemmasFromText(String text);
    void processOnePage(IndexService indexService, Page page);
    void processSite(IndexService indexService, Site site);
    //void process(IndexService indexService, Site site);
    List<String> getWords(String text);
    boolean isNotWord(List<String> words);
    List<Lemma> findLemmaByName(String word, Site site);
}
