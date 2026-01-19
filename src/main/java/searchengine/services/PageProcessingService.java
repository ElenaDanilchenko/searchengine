package searchengine.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.Model.LemmaEntity;
import searchengine.Model.PageEntity;
import searchengine.Model.SiteEntity;
import searchengine.dto.indexing.PageData;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class PageProcessingService {

    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final LemmaFinder lemmaFinder;

    public void processPage(SiteEntity siteEntity, PageData pageData) {
        PageEntity pageEntity = pageService.create(siteEntity, pageData);
        if (pageEntity.getCode() >= 400) {
            return;
        }
        String pageText = lemmaFinder.cleanHtmlTags(pageEntity.getContent());
        Map<String, Integer> lemmasOnPage = lemmaFinder.collectLemmas(pageText);
        lemmaService.merge(lemmasOnPage.keySet(), siteEntity);
        List<LemmaEntity> lemmasEntitiesOnPage = lemmaService.getLemmasEntitiesOnSite(lemmasOnPage.keySet(), siteEntity);

        for (LemmaEntity lemmaEntity : lemmasEntitiesOnPage) {
            indexService.create(pageEntity, lemmaEntity, lemmasOnPage.get(lemmaEntity.getLemma()));
        }
    }
}
