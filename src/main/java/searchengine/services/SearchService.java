package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.LemmaData;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResultItem;
import searchengine.exception.IndexingException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final LemmaFinder lemmaFinder;
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final SnippetGenerator snippetGenerator;
    private final SitesList sitesList;

    public SearchResponse getSearchResult(String siteUrl, String query, int offset, int limit) {
        List<String> sitesUrls = new ArrayList<>();
        if (siteUrl == null) {
            for (Site site : sitesList.getSites()) {
                sitesUrls.add(site.getUrl());
            }
        } else {
            sitesUrls.add(siteUrl);
        }
        List<SiteEntity> siteEntities = new ArrayList<>();
        for (String url : sitesUrls) {
            SiteEntity siteEntity = siteService.findByUrl(url).orElse(null);
            if (siteEntity == null || siteEntity.getStatus() != Status.INDEXED) {
                throw new IndexingException("Сайт " + url + " еще не проиндексирован");
            }
            siteEntities.add(siteEntity);
        }

        Set<String> lemmasInQuery = lemmaFinder.collectLemmas(query).keySet();

        List<SearchResultItem> data = new ArrayList<>();
        for (SiteEntity siteEntity : siteEntities) {
            data.addAll(findSearchResults(siteEntity, lemmasInQuery));
        }
        if (data.isEmpty()) return getEmptyResult();
        // релевантность пересчитывается в относительную с учетом страниц со всех сайтов
        float maxRelevance = data.stream()
                .map(SearchResultItem::getRelevance)
                .max(Float::compareTo)
                .orElse(1.0f);
        for (SearchResultItem item : data) {
            item.setRelevance(item.getRelevance() / maxRelevance);
        }

        data.sort(Comparator.comparing(SearchResultItem::getRelevance).reversed());
        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setCount(data.size());
        response.setData(data.subList(offset, Math.min(offset + limit, data.size())));
        return response;
    }

    private List<SearchResultItem> findSearchResults(SiteEntity siteEntity, Set<String> lemmas) {
        List<LemmaEntity> siteLemmas = lemmaService.getLemmasEntitiesOnSite(lemmas, siteEntity);
        if (siteLemmas.size() < lemmas.size()) {
            return List.of();
        }

        List<PageEntity> pages = findMatchingPagesOnSite(siteEntity, siteLemmas);

        if (pages.isEmpty()) return List.of();

        Map<PageEntity, Float> absRelevances = new HashMap<>();
        for (PageEntity page : pages) {
            float absRank = indexService.findLemmasRank(page, siteLemmas).stream()
                    .map(LemmaData::rank)
                    .reduce(0.0f, Float::sum);
            absRelevances.put(page, absRank);
        }

        List<SearchResultItem> data = new ArrayList<>();
        for (PageEntity page : pages) {
            Document doc = Jsoup.parse(page.getContent());
            String title = doc.title();
            if (title.isBlank()) {
                title = page.getPath();
            }
            String siteUrl = siteEntity.getUrl();
            String siteNormalized = siteUrl.replaceAll("/$", "");
            SearchResultItem searchResultItem = new SearchResultItem();
            searchResultItem.setSite(siteNormalized);
            searchResultItem.setSiteName(siteEntity.getName());
            searchResultItem.setUri(page.getPath());
            searchResultItem.setTitle(title);
            searchResultItem.setSnippet(snippetGenerator.getSnippet(page.getContent(), lemmas));
            // релевантность пока абсолютная
            searchResultItem.setRelevance(absRelevances.get(page));
            data.add(searchResultItem);
        }
        return data;
    }

    private List<PageEntity> findMatchingPagesOnSite(SiteEntity siteEntity, List<LemmaEntity> lemmas) {
        int pagesCount = pageService.getPagesCountOnSite(siteEntity);
        float threshold = 0.75f * pagesCount;
        List<LemmaEntity> sortedLemmas = lemmas.stream()
                .filter(lemmaEntity -> lemmaEntity.getFrequency() < threshold)
                .sorted(Comparator.comparingInt(LemmaEntity::getFrequency))
                .toList();
        if (sortedLemmas.isEmpty()) return List.of();
        List<PageEntity> pages = null;
        for (LemmaEntity lemmaEntity : sortedLemmas) {
            List<IndexEntity> indexes;
            if (pages == null) {
                indexes = indexService.findByLemma(lemmaEntity);
            } else {
                indexes = indexService.findOnPagesByLemma(pages, lemmaEntity);
            }
            pages = indexes.stream().map(IndexEntity::getPageEntity).toList();
            if (pages.isEmpty()) {
                break;
            }
        }
        return pages;
    }

    private SearchResponse getEmptyResult() {
        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setCount(0);
        response.setData(List.of());
        return response;
    }
}
