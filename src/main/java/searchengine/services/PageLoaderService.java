package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.Model.SiteEntity;
import searchengine.dto.indexing.PageData;
import searchengine.utils.UrlUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PageLoaderService {

    private static final int TIMEOUT_MS = 15 * 1000;

    public PageData loadPage(SiteEntity siteEntity, String fullPath) {
        try {
            Document document = Jsoup.connect(fullPath)
                    .userAgent("HeliontSearchBot")
                    .referrer("http://www.google.com")
                    .timeout(TIMEOUT_MS)
                    .ignoreHttpErrors(true)
                    .get();

            int statusCode = document.connection().response().statusCode();
            String content = document.outerHtml();

            Set<String> linksOnPage = getAllLinksOnPage(document, siteEntity.getUrl());

            return new PageData(fullPath, statusCode, content, linksOnPage);
        } catch (IOException ex) {
            log.warn("Failed to fetch URL: {}, reason: {}", fullPath, ex.getMessage());
        }
        return null;
    }

    private Set<String> getAllLinksOnPage(Document document, String siteUrl) {
        if (document == null) {
            return new HashSet<>();
        }
        return document.select("a[href]")
                .stream()
                .map(childElement -> childElement.attr("abs:href"))
                .filter(link -> UrlUtils.isInternalLink(link, siteUrl))
                .filter(link -> !UrlUtils.isFile(link))
                .collect(Collectors.toSet());
    }

}