package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import searchengine.dto.index.SiteDto;
import searchengine.exception.NullArgException;
import searchengine.model.IndexEntity;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexEntityRepository;
import searchengine.repository.LemmaRepository;

import java.io.IOException;
import java.util.*;


@Slf4j
@RequiredArgsConstructor
public class MorphologyServiceImpl implements MorphologyService {
    private final LuceneMorphology luceneMorphology;
    private final LemmaRepository lemmaRepository;
    private final IndexEntityRepository indexEntityRepository;
    private final IndexService indexService;
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private Site site;

    public MorphologyServiceImpl(IndexService indexService) throws IOException {
        this.luceneMorphology = new RussianLuceneMorphology();
        this.lemmaRepository = indexService.getLemmaRepository();
        this.indexEntityRepository = indexService.getIndexEntityRepository();
        this.indexService = indexService;
    }

    @Override
    public void processOnePage(IndexService indexService, Page page) {
        this.site = page.getSite();
        List<Page> pages = new ArrayList<>();
        pages.add(page);
        process(indexService, pages);
    }

    @Override
    public void processSite(IndexService indexService, Site site) {
        this.site = site;
        List<Page> pages = indexService.findPagesBySite(new SiteDto(site.getUrl(), site.getName()));
        process(indexService, pages);
    }


    private void process(IndexService indexService, List<Page> pages) {

        for (Page page : pages) {
            if (page.getCode() != 200)
                continue;
            String text = page.getContent();
            HashMap<String, Integer> lemmas = getLemmasFromText(stripHtml(text));
            try {
                saveLemmasToDB(lemmas, page);
                saveIndexToDB(lemmas, page);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void saveLemmasToDB(HashMap<String, Integer> lemmasOnPageMap, Page page)
            throws Exception {
        if (page == null) {
            throw new NullArgException("Page is null");
        }
        if (page.getSite() == null) {
            throw new NullArgException("Site is null");
        }
        // получаем список лемм из БД для уменьшения кол-во запросов
        // далее работаем со список лемм (map)
        HashMap<String, Lemma> lemmasOnDBMap = getAllLemmasFromDB(page.getSite());
        List<Lemma> saveLemmasList = new ArrayList<>();
        for (String lemma : lemmasOnPageMap.keySet()) {
            Lemma savedLemma = new Lemma();
            if (lemmasOnDBMap.containsKey(lemma)) {
                // если лемма есть в БД
                savedLemma = lemmasOnDBMap.get(lemma);
                savedLemma.incrementFrequency();
                lemmasOnDBMap.put(lemma, savedLemma);
            } else {
                // если нет в БД создаем и добавляем ее в список для сохранения
                savedLemma.setLemma(lemma);
                savedLemma.setFrequency(1);
                savedLemma.setSite(page.getSite());
                lemmasOnDBMap.put(lemma, savedLemma); // добавляем список БД
            }
            saveLemmasList.add(savedLemma);
        }
        // сохраняем данные в БД пакетом
        lemmaRepository.saveAll(saveLemmasList);
    }

    /**
     * Сохраняет в индекс пулл лемм с указанной в page страницы
     *
     * @param lemmasOnPageMap HashMap<String, Integer> леммы и их кол-во на странице
     * @param page            entity-модель Page, если null то все страницы
     */
    private void saveIndexToDB(HashMap<String, Integer> lemmasOnPageMap, Page page) throws Exception {
        if (page == null) {
            throw new NullArgException("Page is null");
        }
        // сохраняем данные о лемме в index
        HashMap<String, Lemma> lemmasOnDBMap = getAllLemmasFromDB(page.getSite());
        List<IndexEntity> saveIndexEntityList = new ArrayList<>();
        for (String lemmaStr : lemmasOnPageMap.keySet()) {
            IndexEntity indexEntity = new IndexEntity();
            indexEntity.setLemma(lemmasOnDBMap.get(lemmaStr));
            indexEntity.setPage(page);
            indexEntity.setRank((float) lemmasOnPageMap.get(lemmaStr).floatValue());
            saveIndexEntityList.add(indexEntity);
        }
        indexEntityRepository.saveAll(saveIndexEntityList);
    }

    /**
     * Получить из БД все lemmas для указанного сайта, в key вынесена сама лемма
     * для удобства дальнейшей обработки
     *
     * @param site entity-модель Site, если null то все леммы из БД
     * @return Map<String, Lemma> lemmas относящиеся к данному сайту
     */
    private HashMap<String, Lemma> getAllLemmasFromDB(Site site) {
        Lemma lemEx = new Lemma();
        lemEx.setSite(site);
        List<Lemma> lemmaList = lemmaRepository.findAll(Example.of(lemEx));
        // переводим List в Map<String, Lemma> для удобства дальнейшей обработки
        HashMap<String, Lemma> lemmaMap = new HashMap<>();
        for (Lemma lemma : lemmaList) {
            lemmaMap.put(lemma.getLemma(), lemma);
        }
        return lemmaMap;
    }

    private Lemma strToLemma(String lemmaStr, Site site) {
        Lemma lemmaEntity = new Lemma();
        lemmaEntity.setLemma(lemmaStr);
        lemmaEntity.setSite(site);
        return lemmaEntity;
    }

    @Override
    public HashMap<String, Integer> getLemmasFromText(String text) {
        List<String> words = getWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isEmpty() || word.length() < 3) {
                continue;
            }
            // Проверяем является ли слово служебной частью речи, если да отбрасываем
            List<String> wordsBaseForm = luceneMorphology.getMorphInfo(word);
            if (isNotWord(wordsBaseForm)) {
                continue;
            }

            List<String> wordsNormalForm = luceneMorphology.getNormalForms(word);
            if (wordsNormalForm.isEmpty()) {
                continue;
            }

            String nWord = wordsNormalForm.get(0);

            if (lemmas.containsKey(nWord)) {
                lemmas.put(nWord, lemmas.get(nWord) + 1);
            } else {
                lemmas.put(nWord, 1);
            }


        }
        return lemmas;
    }

    @Override
    public List<String> getWords(String text) {
        return Arrays.stream(text.toLowerCase()
                        .replaceAll("([^а-я\s])", " ")
                        .strip()
                        .split("\\s+"))
                .filter(word -> word.length() > 2)
                .toList();
    }

    @Override
    public boolean isNotWord(List<String> words) {
        return words.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Получиь из БД все List<lemmas> согласно запроса.
     * @param word String лемма
     * @param site Site сайт для которого находить леммы, если null то по всем сайтам
     * @return
     */
    @Override
    public List<Lemma> findLemmaByName(String word, Site site) {
        List<Lemma> lemmasList = lemmaRepository.findAll(Example.of(strToLemma(word, site)), Sort.by(Sort.Direction.ASC, "frequency"));
//        lemmasList.stream().forEach(lemma -> log.info("lemma: {} - {}", lemma.getLemma(), lemma.getFrequency()));
        return lemmasList;
    }

    private String stripHtml(String html) {
        return Jsoup.clean(html, Safelist.none());
    }

    // TODO: Удалить после DEBUG
    public void test() {

    }
}
