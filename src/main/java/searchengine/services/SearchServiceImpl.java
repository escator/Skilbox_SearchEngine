package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import searchengine.dto.index.SiteDto;
import searchengine.dto.search.SearchItemData;
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

    private final int OFFSET_SNIPPET_START = 10; // индекс смещения сниппера от ключ.слова влево
    private final int OFFSET_SNIPPET_END = 10; // индекс смещения сниппера от ключ.слова вправо
    private final int MAX_LENGTH_SNIPPET = 300; // максимальная длинна сниппера

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

        Site site = siteService.findSite(null, null, siteUrl);
        // получаем map лемм из строки поиска
        HashMap<String, Integer> lemmasSearchQueryMap = morphologyService.getLemmasFromText(query);
        // удаляем те леммы, которые встречаются слишком часто и
        // заменяем value на колво страниц на которых лемма встретилась
        lemmasSearchQueryMap = removeFerquenterLemmas(lemmasSearchQueryMap, 80.0, siteUrl);
        // сортируем леммы по частоте от мин до макс
        Map<String, Integer> sortMap = sortLemmasMap(lemmasSearchQueryMap);
        List<String> lemmasSortList = sortMap.keySet().stream().toList();
        // отбираем страницы содержащие все леммы из запроса
        List<Page> pages = findPageMatchingQuery(lemmasSortList, site);

        List<SearchPageData> searchingPages = convertPageToSearchPageData(pages, lemmasSortList, site);
        searchingPages = searchingPages.stream()
                .sorted(Comparator.comparingDouble(SearchPageData::getRelRelevance).reversed())
                .toList();

        List<SearchItemData> items = new ArrayList<>();
        for (SearchPageData page : searchingPages) {
            SearchItemData item = new SearchItemData();
            item.setSite(page.getPage().getSite().getUrl());
            item.setSiteName(page.getPage().getSite().getName());
            item.setUri(page.getPage().getPath());
            item.setTitle(page.getTitle());
            item.setSnippet(page.getSnippet());
            item.setRelevance(page.getRelRelevance());
            items.add(item);
        }
        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setCount(items.size());
        response.setData(items);

        //searchingPages.forEach(page -> log.info("id: {}, rel: {} ", page.getPage().getId(), page.getRelRelevance()));
        return response;
    }

    /**
     * Создает сниппет для указанной в объекте SearchPageData страницы.
     * @param pageData экземпляр SearchPageData должен содержать страницу
     *                 для которой будет создаваться сниппет
     * @return
     */
    private String createSnippetForPage(SearchPageData pageData) {
        if (pageData == null || pageData.getPage() == null) {
            return "";
        }
        String html = pageData.getPage().getContent();
        String text = Jsoup.parse(html).body().text();
        String [] textArray = text.split(" ");
        List<String> lemmasStrList = pageData.getLemmas().keySet().stream()
                .map(Lemma::getLemma)
                .toList();

        // Создаем список позиций ключевых слов в тексте
        List<Integer> searchWordsPositions = new ArrayList<>();
        for (int i = 0; i < textArray.length; i++) {
            String currentWord = textArray[i].replaceAll("[^А-я]", " ").toLowerCase().strip();
            if (currentWord.isBlank() || currentWord.length() < 3 || !morphologyService.checkString(currentWord)) {
                continue;
            }
            String normalFormWord = morphologyService.getNormalFormsWord(currentWord);
            if (lemmasStrList.contains(normalFormWord)) {
                searchWordsPositions.add(i);
            }
        }
        return getSnippetFromText(textArray, searchWordsPositions);
    }

    /**
     * Извлекает текст сниппета из массива слов, относительно позиций ключевых слов в списке positions смещаясь на расстояние указанных констант OFFSET_SNIPPET_START и OFFSET_SNIPPET_END. Константа MAX_LENGTH_SNIPPET - максимальная длина сниппета, может варьироваться в сторону превышения, т.к. добавляется фраза, содержащая ключевое слово, целиком. Если после добавления фразы длина привысила MAX_LENGTH_SNIPPET, то следующая фраза не добавляется, а сниппет считается сформированным.
     * @param textArray массив слов всей страницы
     * @param positions список позиций ключевых слов
     * @return
     */
    private String getSnippetFromText(String[] textArray, List<Integer> positions) {
        StringBuilder sb = new StringBuilder();
        for (int pos : positions) {
            int cBefore = 0, cAfter = 0;
            if (pos - OFFSET_SNIPPET_START < 0) {
                cBefore = 0;
            } else {
                cBefore = pos - OFFSET_SNIPPET_START;
            }
            if (pos + OFFSET_SNIPPET_END > textArray.length - 1) {
                cAfter = textArray.length - 1;
            } else {
                cAfter = pos + OFFSET_SNIPPET_END;
            }
            for (int i = cBefore; i < cAfter; i++) {
                if (i == pos) {
                    sb.append("<b>" + textArray[i] + "</b>");
                } else {
                    sb.append(textArray[i]);
                }
                sb.append(" ");
            }
            if (sb.length() > MAX_LENGTH_SNIPPET) {
                break;
            }
        }
        return sb.toString();
    }

    private String getPageTitle(Page page) {
        if (page == null) {
            return "";
        }
        String html = page.getContent();
        Document doc = Jsoup.parse(html);
        return doc.title();
    }

    /**
     * Конвертирует список объектов Page в объекты типа SearchPageData. Для каждого
     * объекта типа SearchPageData, подставляются леммы соответствующие запросу найденные
     * на этой странице, рассчитывается релевантность (abs & rel), Заголовок страницы,
     * создается сниппет.
     * @param pagesList список страниц, которые будут обработаны
     * @param lemmasList список ключевые слова поиска
     * @param site Site объект сайта для которого будет делаться выборка поиска
     * @return список объектов типа SearchPageData, для каждого объекта типа Page
     */
    private List<SearchPageData> convertPageToSearchPageData(List<Page> pagesList, List<String> lemmasList, Site site) {
        if (pagesList.size() < 1) {
            return new ArrayList<>();
        }
        List<SearchPageData> searchingPagesList = new ArrayList<>();
        List<Lemma> lemmas = findAllLemmaByName(lemmasList, site);
        for (Page page : pagesList) {
            SearchPageData searchingPage = new SearchPageData();
            IndexEntity entity = new IndexEntity();
            entity.setPage(page);
            List<IndexEntity> entitiesIndex = indexEntityRepository.findAll(Example.of(entity));
            entitiesIndex = entitiesIndex.stream().filter(e -> lemmas.contains(e.getLemma())).toList();
            Map<Lemma, Double> lemmasMap = new HashMap<>();
            for (IndexEntity e : entitiesIndex) {
                lemmasMap.put(e.getLemma(), e.getRank());
            }

            searchingPage.setPage(page);
            searchingPage.setLemmas(lemmasMap);
            searchingPage.setTitle(getPageTitle(page));
            searchingPage.setSnippet(createSnippetForPage(searchingPage));
            searchingPagesList.add(searchingPage);
        }
        return resolveRelRelevance(searchingPagesList);
    }

    /**
     * Находит в БД страницы соответствующие всем ключевым словам поиска
     * @param lemmasStrings список ключевых слов поиска
     * @param site Site объект сайта для которого будет делаться выборка
     *             null по всей БД
     * @return List<Page> найденные объекты Page соответствующие ключевым словам
     */
    private List<Page> findPageMatchingQuery(List<String> lemmasStrings, Site site) {
        if (lemmasStrings.size() < 1) {
            return new ArrayList<>();
        }
        List<Page> pages = getListPagesFoundLemmas(lemmasStrings.get(0), site);
        for (int i = 1; i < lemmasStrings.size(); i++) {
            List<Page> pagesNext = getListPagesFoundLemmas(lemmasStrings.get(i), site);
            pages = pages.stream()
                    .filter(page -> pagesNext.contains(page))
                    .toList();
        }
        return pages;
    }

    /**
     * Рассчитывает относительную релевантность для объектов SearchPageData из переданного списка. У экземпляров должны быть установленны поля с леммами ключевых слов поиска и поле с absRelevance
     * @param data Список SearchPageData объектов, которые необходимо обработать
     * @return List<SearchPageData> тот же список, но с установленным полем относительной
     * релевантности
     */
    private List<SearchPageData> resolveRelRelevance(List<SearchPageData> data) {
        double maxAbsRelevance = data.stream().map(page -> page.getAbsRelevance()).max(Double::compare).get();
        data.forEach(p -> p.setRelRelevance(p.getAbsRelevance() / maxAbsRelevance));
        return data;
    }

    /**
     * Выбирает из БД страницы соответствующие ключевому слову
     * @param lemmaStr ключевое слово поиска
     * @param site Site объект сайта для которого будет делаться выборка
     *             null по всей БД
     * @return List<Page> найденные объекты Page соответствующие ключевому слову
     */
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
     * Возвращает список всех найденных объектов Lemma соответствующих ключевым словам
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
     * Возвращает список объектов Lemma соответствующих ключевому слову
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
        int allPageCount = siteService.countPagesOnSite(siteDto);
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
     * Количество страниц на которых присутствует ключевое слово
     *
     * @param lemmaStr слово для выбора
     * @param site     Site объект сайта для которого будет делаться выборка
     *                 null по всей БД
     * @return количество страниц которые содержат слово
     */
    private int countPageFoundLemmas(String lemmaStr, Site site) {
        int countLemmas = 0;
        List<Lemma> lemmaList = findLemmaByName(lemmaStr, site);
        for (Lemma lemma : lemmaList) {
            countLemmas += lemma.getFrequency();
        }
        return countLemmas;
    }

}
