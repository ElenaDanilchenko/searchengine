package searchengine.utils;

import lombok.experimental.UtilityClass;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

@UtilityClass
public class UrlUtils {

    private static final Set<String> FILE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "pdf", "doc", "docx", "gif", "rar", "zip", "xls", "xlsx", "webp"
    );

    public String getRelativePath(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                return "/";
            }
            if (!path.contains(".") && !path.endsWith("/")) {
                return path + "/";
            }
            return path;
        } catch (URISyntaxException e) {
            return "/";
        }
    }

    public URI parseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL пуст");
        }
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null || !uri.getScheme().equalsIgnoreCase("http")
                    && !uri.getScheme().equalsIgnoreCase("https")) {
                throw new IllegalArgumentException("Поддерживаются только http/https");
            }
            if (uri.getHost() == null) {
                throw new IllegalArgumentException("Host пуст");
            }

            return uri;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Некорректный url");
        }
    }

    public boolean isInternalLink(String url, String siteUrl) {
        try {
            URI linkUri = new URI(url);
            URI siteUri = new URI(siteUrl);
            return linkUri.getHost() != null &&
                    linkUri.getHost().equals(siteUri.getHost()) &&
                    linkUri.getFragment() == null &&
                    linkUri.getQuery() == null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public boolean isFile(String url) {
        int lastDot = url.lastIndexOf('.');
        if (lastDot == -1) return false;
        String ext = url.substring(lastDot + 1).toLowerCase();
        return FILE_EXTENSIONS.contains(ext);
    }
}
