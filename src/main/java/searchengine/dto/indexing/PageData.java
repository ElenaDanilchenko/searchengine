package searchengine.dto.indexing;

import java.util.Set;

public record PageData(String fullPath, int statusCode, String content, Set<String> linksOnPage) {
}
