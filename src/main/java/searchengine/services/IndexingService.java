package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.Model.SiteEntity;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.PageData;
import searchengine.exception.IndexingException;
import searchengine.utils.UrlUtils;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;

@RequiredArgsConstructor
@Service
@Slf4j
public class IndexingService {

    private final SitesList sitesList;
    private final SiteService siteService;
    private final PageService pageService;
    private final PageLoaderService pageLoaderService;
    private final PageProcessingService pageProcessingService;

    private final List<CompletableFuture<Void>> futures = new ArrayList<>();
    private final List<ForkJoinPool> pools = new ArrayList<>();
    private final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private ScheduledExecutorService updatingExecutor;

    public void index() {

        if (!isIndexing.compareAndSet(false, true)) {
            throw new IndexingException("Индексация уже запущена");
        }

        futures.clear();
        pools.clear();

        updatingExecutor = Executors.newSingleThreadScheduledExecutor();

        int sitesAmount = sitesList.getSites().size();

        // количество потоков делится поровну между сайтами, один поток остается для обновления статуса
        int threadsPerSite = Math.max((Runtime.getRuntime().availableProcessors() - 1) / sitesAmount, 1);

        for (Site site : sitesList.getSites()) {
            siteService.clearTables(site);
            SiteEntity siteEntity = siteService.create(site);
            AtomicBoolean siteIndexingInProcess = new AtomicBoolean(true);
            Runnable updatingStatusTask = () -> {
                if (siteIndexingInProcess.get()) {
                    siteService.updateStatusTime(siteEntity);
                }
            };
            updatingExecutor.scheduleAtFixedRate(updatingStatusTask, 1, 1, SECONDS);
            CompletableFuture<Void> siteFuture = CompletableFuture.runAsync(() -> {
                try (ForkJoinPool pool = new ForkJoinPool(threadsPerSite)) {
                    pools.add(pool);
                    log.info("Начинается индексация сайта {}", siteEntity.getName());
                    HtmlParser siteParser = new HtmlParser(siteEntity, siteEntity.getUrl(), pageService,
                            pageLoaderService, pageProcessingService);
                    pool.invoke(siteParser);
                    log.info("Завершена индексация сайта {}", siteEntity.getName());
                    siteService.markIndexed(siteEntity);
                } catch (CancellationException e) {
                    log.info("Индексация сайта {} остановлена", siteEntity.getName());
                    siteService.markFailed(siteEntity, "Индексация остановлена пользователем");
                } catch (Exception e) {
                    log.error("Ошибка индексации: ", e);
                    siteService.markFailed(siteEntity, e.getMessage());
                } finally {
                    siteIndexingInProcess.set(false);
                }
            });
            futures.add(siteFuture);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((result, ex) -> {
                    isIndexing.set(false);
                    updatingExecutor.shutdown();
                });
    }

    public void stopIndex() {
        if (!isIndexing.compareAndSet(true, false)) {
            throw new IndexingException("Индексация не запущена");
        }
        futures.forEach(siteFuture -> siteFuture.cancel(true));
        pools.forEach(ForkJoinPool::shutdownNow);
        updatingExecutor.shutdown();
    }

    public IndexingResponse getIndexingResponse() {
        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }

    public void indexPage(String url) {
        URI uri = UrlUtils.parseUrl(url);
        String siteUrl = uri.getScheme() + "://" + uri.getHost() + "/";

        Site site = findSiteByUrlInSitesList(siteUrl);

        if (site == null) {
            throw new IndexingException("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        CompletableFuture.runAsync(() -> {
            log.info("Начинается индексация страницы {}", url);
            log.info("Поток: {}", Thread.currentThread().getName());

            siteService.findByUrl(siteUrl)
                    .flatMap(siteEntity -> pageService.findPage(siteEntity, url))
                    .ifPresent(pageService::deletePage);

            SiteEntity siteEntity = siteService.getOrCreate(site);
            PageData pageData = pageLoaderService.loadPage(siteEntity, url);

            if (pageData == null) {
                log.info("Не удалось загрузить страницу {}", url);
                return;
            }

            pageProcessingService.processPage(siteEntity, pageData);

            log.info("Завершена индексация страницы {}", url);
        });
    }

    public boolean isIndexing() {
        return isIndexing.get();
    }

    private Site findSiteByUrlInSitesList(String url) {
        return sitesList.getSites().stream()
                .filter(site -> site.getUrl().equals(url))
                .findFirst()
                .orElse(null);
    }
}
