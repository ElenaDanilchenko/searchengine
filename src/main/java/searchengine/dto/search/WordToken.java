package searchengine.dto.search;

import java.util.List;

public record WordToken(String word, int start, int end, List<String> lemmas) {
}
