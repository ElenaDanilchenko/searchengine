package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import searchengine.model.SiteEntity;
import searchengine.dto.indexing.PageData;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RecursiveAction;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@RequiredArgsConstructor
public class HtmlParser extends RecursiveAction {

    private final SiteEntity siteEntity;
    private final String path;
    private final PageService pageService;
    private final PageLoaderService pageLoaderService;
    private final PageProcessingService pageProcessingService;

    private final Set<String> linksOnPage = new HashSet<>();

    private static final int REQUEST_DELAY_MS = 100;

    @Override
    protected void compute() {
        if (currentThread().isInterrupted()) {
            return;
        }
        List<HtmlParser> subtasks = new ArrayList<>();
        if (!pageService.isNewPage(siteEntity, path)) {
            return;
        }
        try {
            MILLISECONDS.sleep(REQUEST_DELAY_MS);
            PageData pageData = pageLoaderService.loadPage(siteEntity, path);
            if (pageData == null) {
                return;
            }
            linksOnPage.addAll(pageData.linksOnPage());
            try {
                pageProcessingService.processPage(siteEntity, pageData);
            } catch (DataIntegrityViolationException ex) {
                return;
            }

            for (String childLink : linksOnPage) {
                if (currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                if (pageService.isNewPage(siteEntity, childLink)) {
                    HtmlParser subtask = new HtmlParser(siteEntity, childLink, pageService, pageLoaderService, pageProcessingService);
                    subtask.fork();
                    subtasks.add(subtask);
                }
            }
            for (HtmlParser subtask : subtasks) {
                subtask.join();
            }
        } catch (InterruptedException ex) {
            currentThread().interrupt();
            throw new CancellationException();
        }
    }
}
