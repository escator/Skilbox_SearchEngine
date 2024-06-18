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
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
public class MorphologyServiceImpl implements MorphologyService {
    private final LuceneMorphology luceneMorphology;
    private final LemmaRepository lemmaRepository;
    private final IndexService indexService;
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private Site site;

    public MorphologyServiceImpl(IndexService indexService) throws IOException {
        this.luceneMorphology = new RussianLuceneMorphology();
        this.lemmaRepository = indexService.getLemmaRepository();
        this.indexService = indexService;
    }

    @Override
    public void process(IndexService indexService, Site site) {
        this.site = site;
        List<Page> pages = indexService.findPagesBySite(new SiteDto(site.getUrl(), site.getName()));

        for  (Page page: pages)  {
            String text = page.getContent();
            HashMap<String, Integer> lemmas  = getLemmas(stripHtml(text));
            lemmas.forEach((key, value) -> {saveLemmasToDB(key);});
        }
    }

    private void saveLemmasToDB(String lemma) {
        Optional<Lemma> lemmaOptional  = lemmaRepository.findOne(Example.of(getLemmaFromStr(lemma)));
        if (lemmaOptional.isEmpty()) {
            Lemma newLemma = new Lemma();
            newLemma.setLemma(lemma);
            newLemma.setFrequency(1);
            newLemma.setSite(site);
            lemmaRepository.save(newLemma);
        } else {
            lemmaOptional.get().incrementFrequency();
            lemmaRepository.save(lemmaOptional.get());
            log.info("Lemma not found: {}", lemma);
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
            if (word.isEmpty()) {
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
