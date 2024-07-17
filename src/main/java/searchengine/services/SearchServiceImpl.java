package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import searchengine.dto.index.SiteDto;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexEntityRepository;
import searchengine.repository.LemmaRepository;
import searchengine.response.SearchResponse;
import searchengine.util.LinkToolsBox;
import searchengine.util.SiteToolsBox;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final IndexService indexService;
    private final SiteService siteService;
    //private final PageService pageService;
    private final MorphologyService morphologyService;
    private final LemmaRepository lemmaRepository;
    private final IndexEntityRepository indexEntityRepository;

    public SearchServiceImpl(IndexService indexService,
                             SiteService siteService,
                             LemmaRepository lemmaRepository) throws Exception {
        this.indexService  = indexService;
        this.morphologyService = new MorphologyServiceImpl(indexService);
        this.lemmaRepository = lemmaRepository;
        this.indexEntityRepository  = indexService.getIndexEntityRepository();
        this.siteService = siteService;
        //this.pageService = pageService;

    }

    @Override
    public SearchResponse search(String query, int offset, int limit, String siteUrl) {
        log.info("Searching for {}", query);
        // получаем map лемм из
        HashMap<String, Integer> lemmasSearchQueryMap = morphologyService.getLemmasFromText(query);
        // удаляем те леммы, которые встречаются слишком часто
        lemmasSearchQueryMap = removeFerquenterLemmas(lemmasSearchQueryMap, 65.0, siteUrl);
        // сортируем леммы по частоте от мин до макс
        Map<String, Integer> sortMap = sortLemmasMap(lemmasSearchQueryMap);

        for (String lemma : sortMap.keySet()) {
            log.info("Lemma {} is {}", lemma, lemmasSearchQueryMap.get(lemma));
            List<Lemma> lemmas = findLemmaByName(lemma, siteService.findSite(null, null, siteUrl));
            for (Lemma lemm : lemmas) {
                log.info("Lemma - {}, site: {}", lemm.getLemma(), lemm.getSite().getUrl());
            }
        }

        return new SearchResponse();
    }

    private List<Lemma> findLemmaByName(String lemmaStr, Site site) {
        Lemma exLemma = new Lemma(lemmaStr, site);
        return lemmaRepository.findAll(Example.of(exLemma));

    }



    private Map<String, Integer> sortLemmasMap(Map<String, Integer> lemmasMap) {
        return lemmasMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue()).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private List<Page> getAllPageFoundLemmas(HashMap<String, Integer> lemmasMap, String siteUrl) {

        return null;
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

        Site site = siteService.findSite(null, null, siteUrl);
        SiteDto siteDto = SiteToolsBox.siteModelToSiteDto(site);
        int allPageCount = siteService.getPagesCount(siteDto);
        for (String lemma : lemmasMap.keySet()) {
            int countLemmas = countPageFoundLemmas(lemma, site);
            if (((double)countLemmas / allPageCount * 100) > limitPercent) {
                lemmasMap.remove(lemma);
            } else {
                lemmasMap.put(lemma, countLemmas);
            }
        }
        return lemmasMap;
    }


    /**
     * Количество страниц на которые содержат переданную лемму
     * @param lemmaStr слово для выбора
     * @return количество страниц которые содержат слово
     */
    private int countPageFoundLemmas(String lemmaStr, Site site) {
        int countLemmas = 0;
        List<Lemma> lemmaList = morphologyService.findLemmaByName(lemmaStr, site);
        for (Lemma lemma : lemmaList) {
            countLemmas += lemma.getFrequency();
        }
        return countLemmas;
    }

}
