package searchengine.dto.search;

import lombok.Data;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SearchPageData {
    public SearchPageData() {
        this.lemmas = new HashMap<Lemma, Double>();
        this.absRelevance = 0.0;
        this.relRelevance = 0.0;
    }

    private Page page;
    private Map<Lemma, Double> lemmas;
    private double absRelevance;
    private double relRelevance;

    public void setLemmas(Map <Lemma, Double> lemmas) {
        this.lemmas = lemmas;
        lemmas.forEach((lemma, rank) -> absRelevance += rank);

    }
}
