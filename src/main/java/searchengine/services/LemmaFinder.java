package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class LemmaFinder {

    private final LuceneMorphology luceneMorphology;

    public Map<String, Integer> collectLemmas(String text) {
        String[] words = getCyrillicWords(text);
        Map<String, Integer> result = new HashMap<>();

        for (String word : words) {

            if (word.isBlank()) {
                continue;
            }

            List<String> wordNormalFormsInfo = luceneMorphology.getMorphInfo(word);
            for (String wordInfo : wordNormalFormsInfo) {
                if (isFunctionWord(wordInfo)) {
                    continue;
                }
                String baseForm = wordInfo.split("\\|")[0];
                result.merge(baseForm.replaceAll("ё", "е"), 1, Integer::sum);
            }

        }

        return result;
    }

    public String cleanHtmlTags(String htmlText) {
        return Jsoup.parse(htmlText).text();
    }

    private String[] getCyrillicWords(String text) {
        String nonCyrillic = "[^а-яё\\s]+";
        return text.toLowerCase(Locale.ROOT)
                .replaceAll(nonCyrillic, "")
                .trim()
                .split("\\s+");
    }

    private boolean isFunctionWord(String wordMorphInfo) {
        Set<String> functionWords = Set.of("МЕЖД", "ПРЕДЛ", "СОЮЗ");
        for (String part : functionWords) {
            if (wordMorphInfo.contains(part)) {
                return  true;
            }
        }
        return false;
    }


}
