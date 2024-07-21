package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import searchengine.dto.index.SiteDto;
import searchengine.dto.search.SearchPageData;
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
        // отбираем страницы содержащие все леммы из запроса
        List<Page> pages = findPageMatchingQuery(lemmasSortList, site);

        List<SearchPageData> searchingPages = convertPageToSearchPageData(pages, lemmasSortList, site);


        // pages.stream().forEach(page -> log.info("id: {}, url: {}", page.getId(), page.getPath()));
        searchingPages.forEach(page -> log.info("id: {}, abs: {} rel: {}", page.getPage().getPath(), page.getAbsRelevance(), page.getRelRelevance()));
        return new SearchResponse();
    }

    private List<SearchPageData> convertPageToSearchPageData(List<Page> pagesList, List<String> lemmasList, Site site) {
        List<SearchPageData> searchingPages = new ArrayList<>();
        List<Lemma> lemmas = findAllLemmaByName(lemmasList, site);
        for (Page page : pagesList) {
            SearchPageData searchingPage = new SearchPageData();
            IndexEntity entity = new IndexEntity();
            entity.setPage(page);
            List<IndexEntity> entitiesIndex = indexEntityRepository.findAll(Example.of(entity));
            entitiesIndex = entitiesIndex.stream().filter(e -> lemmas.contains(e.getLemma())).toList();
            Map<Lemma, Double> lem = new HashMap<>();
            for (IndexEntity e : entitiesIndex) {
                lem.put(e.getLemma(), e.getRank());
            }

            searchingPage.setPage(page);
            searchingPage.setLemmas(lem);
            searchingPages.add(searchingPage);
        }
        return resolveRelRelevance(searchingPages);
    }

    private List<Page> findPageMatchingQuery(List<String> lemmasStrings, Site site) {
        List<Page> pages = getListPagesFoundLemmas(lemmasStrings.get(0), site);
        for (int i = 1; i < lemmasStrings.size(); i++) {
            List<Page> pagesNext = getListPagesFoundLemmas(lemmasStrings.get(i), site);
            pages = pages.stream()
                    .filter(page -> pagesNext.contains(page))
                    .toList();
        }
        return pages;
    }

    private List<SearchPageData> resolveRelRelevance(List<SearchPageData> data) {
        double maxAbsRelevance = data.stream().map(page -> page.getAbsRelevance()).max(Double::compare).get();
        data.forEach(p -> p.setRelRelevance(p.getAbsRelevance() / maxAbsRelevance));
        return data;
    }

    private List<Page> getListPagesFoundLemmas(String lemmaStr, Site site) {
        List<IndexEntity> entitiesIndex = new ArrayList<>();
        List<Lemma> lemmasList = findLemmaByName(lemmaStr, site);
        for (Lemma lemma : lemmasList) {
            IndexEntity entity = new IndexEntity();
            entity.setLemma(lemma);
            entitiesIndex.addAll(indexEntityRepository.findAll(Example.of(entity)));
        }

        return entitiesIndex.stream()
                .map(IndexEntity::getPage)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает список все обектов Lemma по всем строковым представлениям
     * переданным в списке lemmaStr
     * @param lemmaStr List<String> список строковых представлений для поиска
     * @param site Site объект сайта для которого будет делаться выборка
     *             null поиск по всей БД
     * @return List<Lemma> найденные объекты леммы
     */
    private List<Lemma> findAllLemmaByName(List<String> lemmaStr, Site site) {
        List<Lemma> lemmasList = new ArrayList<>();
        for (String lemma : lemmaStr) {
            lemmasList.addAll(findLemmaByName(lemma, site));
        }
        return lemmasList;
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
