package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class LemmaFinder {

    private final LuceneMorphology luceneMorphology;

    public Map<String, Integer> collectLemmas(String text) {
        List<String> words = getCyrillicWords(text);
        Map<String, Integer> result = new HashMap<>();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            List<String> lemmas = getLemmas(word);
            for (String lemma : lemmas) {
                result.merge(lemma, 1, Integer::sum);
            }
        }
        return result;
    }

    public List<String> getLemmas(String word) {
        List<String> result = new ArrayList<>();
        List<String> wordNormalFormsInfo = luceneMorphology
                .getMorphInfo(word.toLowerCase(Locale.of("ru", "RU")));
        for (String wordInfo : wordNormalFormsInfo) {
            if (isFunctionWord(wordInfo)) {
                continue;
            }
            String baseForm = wordInfo.split("\\|")[0];
            result.add(baseForm.replaceAll("ё", "е"));
        }
        return result;
    }

    public String cleanHtmlTags(String htmlText) {
        return Jsoup.parse(htmlText).text();
    }

    private List<String> getCyrillicWords(String text) {
        List<String> result = new ArrayList<>();
        String cyrillicPattern = "[а-яё]+(?:-[а-яё]+)*";
        int flags = Pattern.CASE_INSENSITIVE  | Pattern.UNICODE_CASE;
        Pattern pattern = Pattern.compile(cyrillicPattern, flags);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }

    private boolean isFunctionWord(String wordMorphInfo) {
        Set<String> functionWords = Set.of("МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ");
        for (String part : functionWords) {
            if (wordMorphInfo.contains(part)) {
                return  true;
            }
        }
        return false;
    }
}