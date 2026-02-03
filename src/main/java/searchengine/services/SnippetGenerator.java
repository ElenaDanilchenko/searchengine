package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.dto.search.WordToken;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class SnippetGenerator {

    private final LemmaFinder lemmaFinder;

    private static final int SNIPPET_SIZE = 300;

    public String getSnippet(String pageContent, Set<String> queryLemmas) {
        String text = lemmaFinder.cleanHtmlTags(pageContent);
        List<WordToken> tokens = new ArrayList<>();
        String cyrillicPattern = "[а-яё]+(?:-[а-яё]+)*";
        int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        Pattern pattern = Pattern.compile(cyrillicPattern, flags);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String word = matcher.group();
            List<String> wordLemmas = lemmaFinder.getLemmas(word);
            if (!Collections.disjoint(wordLemmas, queryLemmas)) {
                WordToken token = new WordToken(word, matcher.start(), matcher.end(), wordLemmas);
                tokens.add(token);
            }
        }
        List<WordToken> snippetTokens = findBestWindowTokens(tokens);
        return createSnippet(snippetTokens, text);
    }

    private String createSnippet(List<WordToken> snippetTokens, String text) {
        int sizeByTokens = snippetTokens.getLast().end() - snippetTokens.getFirst().start();
        int padding = (SNIPPET_SIZE - sizeByTokens) / 2;
        int rawStart = Math.max(snippetTokens.getFirst().start() - padding, 0);
        int rawEnd = Math.min(snippetTokens.getLast().end() + padding, text.length());
        int finalStart = (rawStart == 0 || text.charAt(rawStart - 1) == ' ')
                ? rawStart
                : text.indexOf(" ", rawStart);
        int finalEnd = (rawEnd == text.length() || !Character.isLetter(text.charAt(rawEnd)))
                ? rawEnd
                : text.lastIndexOf(" ", rawEnd);
        String prefix = (finalStart == 0 || (finalStart >= 2 && text.charAt(finalStart - 2) == '.'))
                ? ""
                : "…";
        String suffix = (finalEnd == text.length() || text.charAt(finalEnd) == '.')
                ? "."
                : "…";
        String snippet = text.substring(finalStart, finalEnd);
        StringBuilder snippetBuilder = new StringBuilder(snippet);
        for (int i = snippetTokens.size() - 1; i >= 0; i--) {
            WordToken token = snippetTokens.get(i);
            snippetBuilder.replace(token.start() - finalStart, token.end() - finalStart,
                    "<b>" + token.word() + "</b>");
        }
        return prefix + snippetBuilder + suffix;
    }

    private List<WordToken> findBestWindowTokens(List<WordToken> tokens) {
        List<WordToken> bestWindow = new ArrayList<>();
        int maxUniqueLemmas = 0;
        int minDistance = Integer.MAX_VALUE;
        int windowSize = SNIPPET_SIZE - 50;
        for (int i = 0; i < tokens.size(); i++) {
            int currentWindowStart = tokens.get(i).start();
            int currentWindowEnd = currentWindowStart + windowSize;
            List<WordToken> windowTokens = collectTokensInWindow(tokens, currentWindowStart, currentWindowEnd);
            List<WordToken> uniqueWindowTokens = getUniqueTokens(windowTokens);
            int uniqueCount = uniqueWindowTokens.size();
            int distance = uniqueWindowTokens.getLast().start() - uniqueWindowTokens.getFirst().start();
            if (uniqueCount > maxUniqueLemmas ||
                    (uniqueCount != 1 && uniqueCount == maxUniqueLemmas && distance < minDistance)) {
                maxUniqueLemmas = uniqueCount;
                minDistance = distance;
                bestWindow = windowTokens;
            }
        }
        return bestWindow;
    }

    private List<WordToken> collectTokensInWindow(List<WordToken> tokens, int windowStart, int windowEnd) {
        List<WordToken> result = new ArrayList<>();
        for (WordToken token : tokens) {
            if (token.start() >= windowStart && token.start() <= windowEnd) {
                result.add(token);
            }
            if (token.start() > windowEnd) break;
        }
        return result;
    }

    private List<WordToken> getUniqueTokens(List<WordToken> tokens) {
        Set<String> uniqueLemmas = new HashSet<>();
        List<WordToken> uniqueTokens = new ArrayList<>();
        for (WordToken token : tokens) {
            if (uniqueLemmas.add(token.lemmas().getFirst())) {
                uniqueTokens.add(token);
            }
        }
        return uniqueTokens;
    }
}
