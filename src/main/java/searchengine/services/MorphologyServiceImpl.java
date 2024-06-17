package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MorphologyServiceImpl implements MorphologyService {
    private final LuceneMorphology luceneMorphology;
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};


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

    // TODO: Удалить после DEBUG
    public void test() {

    }
}
