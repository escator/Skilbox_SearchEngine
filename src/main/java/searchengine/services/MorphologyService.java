package searchengine.services;

import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.HashMap;
import java.util.List;

public interface MorphologyService {
    HashMap<String, Integer> getLemmasStrFromText(String text);
    void processOnePage(Page page);
    void processSite(Site site);
    //void process(IndexService indexService, Site site);
    List<String> getWords(String text);
    boolean isNotWord(List<String> words);
    List<Lemma> findLemmasByName(String word, Site site);
    String getNormalFormsWord(String word);
    boolean checkString(String text);
}
