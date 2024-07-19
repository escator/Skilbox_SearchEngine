package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import searchengine.dto.index.SiteDto;
import searchengine.dto.search.SearchItemData;
import searchengine.model.IndexEntity;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexEntityRepository;
import searchengine.repository.LemmaRepository;
import searchengine.response.SearchResponse;
import searchengine.util.SiteToolsBox;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final IndexService indexService;
    private final SiteService siteService;
    private final MorphologyService morphologyService;
    private final LemmaRepository lemmaRepository;
    private final IndexEntityRepository indexEntityRepository;

    public SearchServiceImpl(IndexService indexService,
                             SiteService siteService,
                             LemmaRepository lemmaRepository) throws Exception {
        this.indexService = indexService;
        this.morphologyService = new MorphologyServiceImpl(indexService);
        this.lemmaRepository = lemmaRepository;
        this.indexEntityRepository = indexService.getIndexEntityRepository();
        this.siteService = siteService;

    }

    @Override
    public SearchResponse search(String query, int offset, int limit, String siteUrl) {
        log.info("Searching for {}", query);

        Site site = siteService.findSite(null, null, siteUrl);
        // получаем map лемм из строки поиска
        HashMap<String, Integer> lemmasSearchQueryMap = morphologyService.getLemmasFromText(query);
        // удаляем те леммы, которые встречаются слишком часто и
        // заменяем value на колво страниц на которых лемма встретилась
        lemmasSearchQueryMap = removeFerquenterLemmas(lemmasSearchQueryMap, 65.0, siteUrl);
        // сортируем леммы по частоте от мин до макс
        Map<String, Integer> sortMap = sortLemmasMap(lemmasSearchQueryMap);
        List<String> lemmasSortList = sortMap.keySet().stream().toList();

        List<Page> pages = getListPagesFoundLemmas(lemmasSortList.get(0), site);
        for (int i = 1; i < lemmasSortList.size(); i++) {
            List<Page> pagesNext = getListPagesFoundLemmas(lemmasSortList.get(i), site);
            pages = pages.stream()
                    .filter(page -> pagesNext.contains(page))
                    .toList();
        }



        pages.stream().forEach(page -> log.info("id: {}, url: {}", page.getId(), page.getPath()));



        return new SearchResponse();
    }

    private int absRelevance(List<String> lemmasList, List<Page> pagesList) {



        return 0;
    }

    private List<Page> getListPagesFoundLemmas(String lemmaStr, Site site) {
        List<IndexEntity> entitiesIndex = new ArrayList<>();
        List<Lemma> lemmasList = findLemmaByName(lemmaStr, site);
        for (Lemma lemma : lemmasList) {
            IndexEntity entity = new IndexEntity();
            entity.setLemma(lemma);
            entitiesIndex.addAll(indexEntityRepository.findAll(Example.of(entity)));
        }
        if (site != null) {
            entitiesIndex = entitiesIndex.stream()
                    .filter(entity -> entity.getPage().getSite().equals(site))
                    .collect(Collectors.toList());
        }
        return entitiesIndex.stream()
                .map(IndexEntity::getPage)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает список обектов Lemma по строковому представлению
     *
     * @param lemmaStr строковое представление леммы
     * @param site     Site объект сайта для которого будет делаться выборка
     *                 null по всей БД
     * @return List<Lemma> найденные объекты леммы
     */
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

    /**
     * Удаляет из переданного Map леммы, которые встречаются более чем на limitPercent страницах
     * всех проиндексированных сайтов (siteUrl=null) или конкретного сайта
     *
     * @param lemmasMap
     * @param limitPercent max percent
     * @return
     */
    private HashMap<String, Integer> removeFerquenterLemmas(
            HashMap<String, Integer> lemmasMap, double limitPercent, String siteUrl) {

        Site site = siteService.findSite(null, null, siteUrl);
        SiteDto siteDto = SiteToolsBox.siteModelToSiteDto(site);
        int allPageCount = siteService.countPagesFromSite(siteDto);
        for (String lemma : lemmasMap.keySet()) {
            int countLemmas = countPageFoundLemmas(lemma, site);
            if (((double) countLemmas / allPageCount * 100) > limitPercent) {
                lemmasMap.remove(lemma);
            } else {
                lemmasMap.put(lemma, countLemmas);
            }
        }
        return lemmasMap;
    }


    /**
     * Количество страниц на которые содержат переданную лемму
     *
     * @param lemmaStr слово для выбора
     * @param site     Site объект сайта для которого будет делаться выборка
     *                 null по всей БД
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
