package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startindexing() {
        indexingService.index();
        return ResponseEntity.ok(indexingService.getIndexingResponse());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopindexing() {
        indexingService.stopIndex();
        return ResponseEntity.ok(indexingService.getIndexingResponse());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(String url) {
        indexingService.indexPage(url);
        return ResponseEntity.ok(indexingService.getIndexingResponse());
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(String query, String site, int offset, int limit) {
        return ResponseEntity.ok(searchService.getSearchResult(site, query, offset, limit));
    }


}
