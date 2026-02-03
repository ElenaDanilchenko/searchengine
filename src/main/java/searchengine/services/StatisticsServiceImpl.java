package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final IndexingService indexingService;
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        boolean isIndexing = indexingService.isIndexing();
        total.setIndexing(isIndexing);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());

            siteService.findByUrl(site.getUrl()).ifPresentOrElse(
                    siteEntity -> fillItemFromEntity(item, siteEntity),
                    () -> fillItemAsEmpty(item)
            );

            total.setPages(total.getPages() + item.getPages());
            total.setLemmas(total.getLemmas() + item.getLemmas());
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private void fillItemFromEntity(DetailedStatisticsItem item, SiteEntity siteEntity) {
        int pages = pageService.getPagesCountOnSite(siteEntity);
        int lemmas = lemmaService.getLemmasCountOnSite(siteEntity);
        item.setPages(pages);
        item.setLemmas(lemmas);
        item.setStatus(siteEntity.getStatus().name());
        item.setError(siteEntity.getLastError() == null ? "" : siteEntity.getLastError());
        item.setStatusTime(siteEntity.getStatusTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli());
    }

    private void fillItemAsEmpty(DetailedStatisticsItem item) {
        item.setPages(0);
        item.setLemmas(0);
        item.setStatus(Status.FAILED.name());
        item.setError("Индексация не начата");
        item.setStatusTime(System.currentTimeMillis());
    }
}
