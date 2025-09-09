package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.Model.SiteEntity;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exception.IndexingException;

import java.util.ArrayList;
import java.util.List;
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
        int threadsPerSite = Math.max(Runtime.getRuntime().availableProcessors() / sitesAmount - 1, 1);

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
                    HtmlParser siteParser = new HtmlParser(siteEntity, siteEntity.getUrl(), pageService, siteService);
                    pool.invoke(siteParser);
                    log.info("Завершена индексация сайта {}", siteEntity.getName());
                    siteService.markIndexed(siteEntity);
                    siteIndexingInProcess.set(false);
                } catch (CancellationException e) {
                    siteService.markFailed(siteEntity, "Индексация остановлена пользователем");
                } catch (Exception e) {
                    siteService.markFailed(siteEntity, e.getMessage());
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
    }

    public IndexingResponse getIndexingResponse() {
        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }
}
