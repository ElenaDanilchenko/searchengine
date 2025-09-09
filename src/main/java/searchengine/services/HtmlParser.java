package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.dao.DataIntegrityViolationException;
import searchengine.Model.PageEntity;
import searchengine.Model.SiteEntity;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@RequiredArgsConstructor
@Slf4j
public class HtmlParser extends RecursiveAction {

    private final SiteEntity siteEntity;
    private final String path;
    private  final PageService pageService;
    private final SiteService siteService;

    private final Set<String> linksOnPage = new HashSet<>();

    private static final int REQUEST_DELAY_MS = 100;
    public static final int TIMEOUT_MS = 15 * 1000;
    private static final Set<String> FILE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "pdf", "doc", "docx", "gif", "rar", "zip", "xls", "xlsx", "webp"
    );

    @Override
    protected void compute() {
        if (currentThread().isInterrupted()) {
            return;
        }
        List<HtmlParser> subtasks = new ArrayList<>();
        if (!pageService.isNewPage(siteEntity, getRelativePath(path))) {
            return;
        }
        try {
            PageEntity pageEntity = handlePage();
            if (pageEntity == null) {
                return;
            }
            try {
                pageService.create(pageEntity);
            } catch (DataIntegrityViolationException ex) {
                return;
            }

            for (String childLink : linksOnPage) {
                if (currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                if (pageService.isNewPage(siteEntity, getRelativePath(childLink))) {
                    HtmlParser subtask = new HtmlParser(siteEntity, childLink, pageService, siteService);
                    subtask.fork();
                    subtasks.add(subtask);
                }
            }
            for (HtmlParser subtask : subtasks) {
                subtask.join();
            }
        } catch (InterruptedException ex) {
            siteService.markFailed(siteEntity, "Индексация остановлена пользователем");
            currentThread().interrupt();
        }
    }

    private PageEntity handlePage() throws InterruptedException{

        PageEntity pageEntity = new PageEntity();
        pageEntity.setSiteEntity(siteEntity);
        pageEntity.setPath(getRelativePath(path));

        Document document = getHtmlDocument();
        if (document == null) {
            return null;
        }
        int code = document.connection().response().statusCode();
        String content = document.outerHtml();
        pageEntity.setCode(code);
        pageEntity.setContent(content);
        linksOnPage.addAll(getAllLinksOnPage(document));
        return pageEntity;
    }

    private Document getHtmlDocument() throws InterruptedException {
        Document document = null;
        try {
            MILLISECONDS.sleep(REQUEST_DELAY_MS);
            document = Jsoup.connect(path)
                    .userAgent("HeliontSearchBot")
                    .referrer("http://www.google.com")
                    .timeout(TIMEOUT_MS)
                    .ignoreHttpErrors(true)
                    .get();
        } catch (IOException e) {
            log.warn("Failed to fetch URL: {}, reason: {}", path, e.getMessage());
        }
        return document;
    }

    private Set<String> getAllLinksOnPage(Document document) {
        if (document == null) {
            return new HashSet<>();
        }
        return document.select("a[href]")
                .stream()
                .map(childElement -> childElement.attr("abs:href"))
                .filter(this::isInternalLink)
                .filter(link -> !isFile(link))
                .collect(Collectors.toSet());
    }

    private boolean isInternalLink(String url) {
        try {
            URI linkUri = new URI(url);
            URI siteUri = new URI(siteEntity.getUrl());
            return linkUri.getHost() != null &&
                    linkUri.getHost().equals(siteUri.getHost()) &&
                    linkUri.getFragment() == null &&
                    linkUri.getQuery() == null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private boolean isFile(String url) {
        int lastDot = url.lastIndexOf('.');
        if (lastDot == -1) return false;
        String ext = url.substring(lastDot + 1).toLowerCase();
        return FILE_EXTENSIONS.contains(ext);
    }

    private String getRelativePath(String url) {
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

}
