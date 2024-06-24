package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import searchengine.dto.index.SiteDto;
import searchengine.model.IndexEntity;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexEntityRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


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
        this.indexEntityRepository  = indexService.getIndexEntityRepository();
        this.indexService = indexService;
    }
    @Override
    public void processOnePage(IndexService indexService, Page page) {
        this.site  = page.getSite();
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
//        this.site = site;
//        List<Page> pages = indexService.findPagesBySite(new SiteDto(site.getUrl(), site.getName()));

        for  (Page page: pages)  {
            if (page.getCode() != 200)
                continue;
            String text = page.getContent();
            HashMap<String, Integer> lemmas  = getLemmas(stripHtml(text));
            lemmas.forEach((key, value) -> {saveLemmasToDB(key, value, page);});
        }
    }

    private void saveLemmasToDB(String lemma, int amount, Page page) {
        Optional<Lemma> lemmaOptional  =
                lemmaRepository.findOne(Example.of(getLemmaFromStr(lemma)));
        Lemma savedLemma = new Lemma();;
        if (lemmaOptional.isEmpty()) {
            savedLemma.setLemma(lemma);
            savedLemma.setFrequency(1);
            savedLemma.setSite(site);
        } else {
            lemmaOptional.get().incrementFrequency();
            savedLemma = lemmaOptional.get();
        }
        synchronized (lemmaRepository) {
            savedLemma = lemmaRepository.save(savedLemma);
        }
        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setLemma(savedLemma);
        indexEntity.setPage(page);
        indexEntity.setRank((float)amount);

        synchronized (indexEntityRepository) {
            indexEntityRepository.save(indexEntity);
        }
    }

    private  Lemma getLemmaFromStr(String lemmaStr)  {
        Lemma lemmaEntity = new Lemma();
        lemmaEntity.setLemma(lemmaStr);
        return lemmaEntity;
    }



    @Override
    public HashMap<String, Integer> getLemmas(String text) {
        String[] words = getWords(text);
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

            List<String> wordsNormalForm  = luceneMorphology.getNormalForms(word);
            if (wordsNormalForm.isEmpty())  {
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

    private String[] getWords(String text) {
        return text.toLowerCase()
                .replaceAll("([^а-я\s])", " ")
                .strip()
                .split("\\s+");
    }
    private boolean isNotWord(List<String> words) {
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

    private String stripHtml(String html)  {
        return Jsoup.clean(html, Safelist.none());
    }

    // TODO: Удалить после DEBUG
    public void test() {

    }
}
