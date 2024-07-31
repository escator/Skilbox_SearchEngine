package searchengine.services;

import lombok.Getter;
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
@Getter
public class MorphologyServiceImpl implements MorphologyService {
    private final LuceneMorphology luceneMorphology;
    private final LemmaRepository lemmaRepository;
    private final IndexEntityRepository indexEntityRepository;
    private final IndexService indexService;
    private final SiteService siteService;
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private Site site;

    public MorphologyServiceImpl(IndexService indexService) throws IOException {
        this.luceneMorphology = new RussianLuceneMorphology();
        this.lemmaRepository = indexService.getLemmaRepository();
        this.indexEntityRepository = indexService.getIndexEntityRepository();
        this.siteService = indexService.getSiteService();
        this.indexService = indexService;
    }

    @Override
    public void processOnePage(Page page) {
        this.site = page.getSite();
        List<Page> pages = new ArrayList<>();
        pages.add(page);
        process(pages);
    }

    @Override
    public void processSite(Site site) {
        this.site = site;
        List<Page> pages = siteService.findPagesBySite(new SiteDto(site.getUrl(), site.getName()));
        process(pages);
    }


    private void process(List<Page> pages) {
        try {
//            List<Lemma> lemmasToSave = makeLemmasListForSave(pages);
            log.info("Save lemmas to DB");
            lemmaRepository.saveAll(makeLemmasListForSave(pages));
//            List<IndexEntity> indexEntitiesToSave = makeIndexListForSave(pages);
            log.info("Save index entities to DB");
            indexEntityRepository.saveAll(makeIndexListForSave(pages));
        } catch (Exception e) {
            // TODO прописать обработку ошибки
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Получает из списка страниц все леммы и подсчитывает их frequency. Создает
     * объекты Lemma и сохраняет их в Map<String, Lemma>.
     * @param pages список страниц объекты Page
     * @return Map<String, Lemma> где ключ - строковое представление леммы
     *                            value - объект Lemma готовый для сохранения в БД
     */
    private Map<String, Lemma> countLemmasFreqOnAllPages(List<Page> pages) {
        Map<String, Lemma> mapLemmasOnAllPages = new HashMap<>();
        for (Page page : pages) {
            if (page.getCode() != 200)
                continue;

            HashMap<String, Integer> lemmasFromText = getLemmasStrFromText(stripHtml(page.getContent()));

            for (String curLemmaStr : lemmasFromText.keySet()) {
                Lemma tLemma = new Lemma();
                if (mapLemmasOnAllPages.containsKey(curLemmaStr)) {
                    tLemma = mapLemmasOnAllPages.get(curLemmaStr);
                    tLemma.setFrequency(tLemma.getFrequency() + 1);
                    mapLemmasOnAllPages.put(curLemmaStr, tLemma);
                } else {
                    tLemma.setLemma(curLemmaStr);
                    tLemma.setFrequency(1);
                    tLemma.setSite(site);
                    mapLemmasOnAllPages.put(curLemmaStr, tLemma);
                }
            }
        }
        return mapLemmasOnAllPages;
    }

    /**
     * Получает все леммы из списка страниц переданного на вход, сопоставляет из с БД
     * на предмет встречемости на странице, при необходимости увеличивает значение
     * и возвращает List<Lemma> готовый для сохранения в БД
     * @param pages список страниц объекты Page, из тела которых будут извлекаться леммы
     *              и подготавливаться к сохранению в БД
     * @return List<Lemma> готовый для сохранения в БД
     * @throws Exception если не установлена локальная переменная site, не возможно сохранить
     * если не установлена локальная переменная site
     */
    private List<Lemma> makeLemmasListForSave(List<Page> pages) throws Exception {
        if (site == null) {
            throw new NullArgException("Site is null");
        }
        Map<String, Lemma> mapLemmasOnAllPages = countLemmasFreqOnAllPages(pages);
        // получаем список всех лемм из БД одним запросом
        // далее работаем со список лемм (map)
        HashMap<String, Lemma> mapLemmasInDB = getAllLemmasFromDB(site);
        List<Lemma> listForSaveOut = new ArrayList<>();

        for (String lemmaStr : mapLemmasOnAllPages.keySet()) {
            Lemma lemma = new Lemma();
            if (mapLemmasInDB.containsKey(lemmaStr)) {
                lemma = mapLemmasInDB.get(lemmaStr);
                lemma.setFrequency(mapLemmasInDB.get(lemmaStr).getFrequency() + lemma.getFrequency());
                listForSaveOut.add(lemma);
            } else {
                lemma.setLemma(lemmaStr);
                lemma.setFrequency(mapLemmasOnAllPages.get(lemmaStr).getFrequency());
                lemma.setSite(site);
                listForSaveOut.add(lemma);
            }
        }
        return listForSaveOut;
    }

    private List<IndexEntity> makeIndexListForSave(List<Page> pages) throws Exception {
        log.info("Создание списка лемм для сохранения в БД");
        List<IndexEntity> listForSaveOut = new ArrayList<>(); // список для сохранения в БД
        for (Page page : pages) {
            HashMap<String, Integer> lemmasOnPageMap = getLemmasStrFromText(stripHtml(page.getContent()));
            for (String lemmaStr : lemmasOnPageMap.keySet()) {
                IndexEntity indexEntity = new IndexEntity();
                indexEntity.setLemma(findLemmaByName(lemmaStr, page.getSite()));
                indexEntity.setPage(page);
                indexEntity.setRank(lemmasOnPageMap.get(lemmaStr).doubleValue());
                listForSaveOut.add(indexEntity);
            }
        }
        log.info("Сохранение списка лемм в БД");
        return listForSaveOut;
    }


    /**
     * Получить из БД все lemmas для указанного сайта, в key вынесена сама лемма
     * для удобства дальнейшей обработки
     *
     * @param site entity-модель Site, если null то все леммы из БД
     * @return Map<String, Lemma> lemmas относящиеся к данному сайту
     *              <Строковое представление, Lemma-object>
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

    /**
     * Получить из переданной строки леммы и посчитать сколько
     * раз она встретилась в данной строке
     *
     * @param text String исходный текст
     * @return HashMap<String, Integer> лемма и кол-во раз она встретилась в тексте
     */
    @Override
    public HashMap<String, Integer> getLemmasStrFromText(String text) {
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
     * Получиь из БД все леммы по имени согласно запроса.
     *
     * @param word String лемма
     * @param site Site сайт для которого находить леммы, если null то по всем сайтам
     * @return
     */
    @Override
    public List<Lemma> findLemmasByName(String word, Site site) {
        List<Lemma> lemmasList = lemmaRepository.findAll(Example.of(strToLemma(word, site)));
        return lemmasList;
    }

    private Lemma findLemmaByName(String word, Site site) {
        Lemma lemma = new Lemma();
        lemma.setLemma(word);
        lemma.setSite(site);
        return lemmaRepository.findOne(Example.of(lemma)).orElse(null);
    }

    private String stripHtml(String html) {
        return Jsoup.clean(html, Safelist.none());
    }

    @Override
    public String getNormalFormsWord(String word) {
        List<String> res = luceneMorphology.getNormalForms(word
                .replaceAll("[^А-я]", " ")
                .toLowerCase()
                .strip());
        return res.get(0);
    }

    @Override
    public boolean checkString(String text) {
        return luceneMorphology.checkString(text);
    }

}
