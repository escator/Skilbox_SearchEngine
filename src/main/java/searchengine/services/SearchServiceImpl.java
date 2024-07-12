package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.index.SiteDto;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.repository.IndexEntityRepository;
import searchengine.repository.LemmaRepository;
import searchengine.response.SearchResponse;
import searchengine.util.SiteToolsBox;

import java.util.*;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final IndexService indexService;
    private final MorphologyService morphologyService;
    private final LemmaRepository lemmaRepository;
    private final IndexEntityRepository indexEntityRepository;

    public SearchServiceImpl(IndexService indexService,
                             LemmaRepository lemmaRepository) throws Exception {
        this.indexService  = indexService;
        this.morphologyService = new MorphologyServiceImpl(indexService);
        this.lemmaRepository = lemmaRepository;
        this.indexEntityRepository  = indexService.getIndexEntityRepository();

    }

    @Override
    public SearchResponse search(String query, int offset, int limit, String siteUrl) {
        log.info("Searching for {}", query);
        HashMap<String, Integer> lemmasSearchQueryMap = morphologyService.getLemmas(query);
        lemmasSearchQueryMap = removeFerquenterLemmas(lemmasSearchQueryMap, 65.0, siteUrl);
        for (String lemma : lemmasSearchQueryMap.keySet()) {
            log.info("Lemma {} is {}", lemma, lemmasSearchQueryMap.get(lemma));
        }
        List<String> lemassSortedList = sortLemmasOnRating(lemmasSearchQueryMap); // сортируем по рейтингу
        for (String lemma : lemassSortedList) {
            log.info("Lemma {}", lemma);
        }
        return new SearchResponse();
    }

    /**
     * Удаляет из переданного Map леммы, которые встречаются более чем на limitPercent страницах
     * всех проиндексированных сайтов (siteUrl=null) или конкретного сайта
     * @param lemmasMap
     * @param limitPercent max percent
     * @return
     */
    private HashMap<String, Integer> removeFerquenterLemmas(
            HashMap<String, Integer> lemmasMap, double limitPercent, String siteUrl) {

        Site site = indexService.findSite(null, null, siteUrl);
        SiteDto siteDto = SiteToolsBox.siteModelToSiteDto(site);
        int allPageCount = indexService.getPagesCount(siteDto);
        for (String lemma : lemmasMap.keySet()) {
            int countLemmas = countPageOnLemmas(lemma);
            if (((double)countLemmas / allPageCount * 100) > limitPercent) {
                lemmasMap.remove(lemma);
            } else {
                lemmasMap.put(lemma, countLemmas);
            }
        }
        return lemmasMap;
    }

    private List<String> getWords(String query) {
        List<String> words = morphologyService.getWords(query);
        for (String word : words) {
            log.info("Words are {}", word);

        }
        return null;
    }

    /**
     * Количество страниц на которые содержат переданную лемму
     * @param lemmaStr слово для выдора
     * @return количество страниц которые содержат слово
     */
    private int countPageOnLemmas(String lemmaStr)  {
        //TODO Посчитать количество страниц на которые содержат слово
        int countLemmas = 0;
        List<Lemma> lemmaList = morphologyService.findLemmaByName(lemmaStr);
        for (Lemma lemma : lemmaList) {
            countLemmas += lemma.getFrequency();
        }
        return countLemmas;
    }

    /**
     * Возвращает сортированный по рейтингу список лемм
     * @param lemmas
     * @return
     */
    private List<String> sortLemmasOnRating(HashMap<String, Integer> lemmas)  {
        // TODO Сортировать слова по рейтингу
        List<String> result = lemmas.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(a -> a.getKey())
                .toList();



        return null;
    }
}
