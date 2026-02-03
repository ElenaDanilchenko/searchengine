package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.dto.search.LemmaData;
import searchengine.repositories.IndexRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexService {

    private final IndexRepository indexRepository;

    @Transactional(readOnly = true)
    public List<Integer> getLemmasIdsOnPage(PageEntity pageEntity) {
        return indexRepository.findLemmasIdsByPageEntity(pageEntity);
    }

    public void create(PageEntity pageEntity, LemmaEntity lemmaEntity, float rank) {
        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setPageEntity(pageEntity);
        indexEntity.setLemmaEntity(lemmaEntity);
        indexEntity.setRank(rank);
        indexRepository.save(indexEntity);
    }

    @Transactional(readOnly = true)
    public List<IndexEntity> findByLemma(LemmaEntity lemmaEntity) {
        return indexRepository.findByLemmaEntity(lemmaEntity);
    }

    @Transactional(readOnly = true)
    public List<IndexEntity> findOnPagesByLemma(List<PageEntity> pages, LemmaEntity lemmaEntity) {
        return indexRepository.findByPageEntityInAndLemmaEntity(pages, lemmaEntity);
    }

    @Transactional(readOnly = true)
    public List<LemmaData> findLemmasRank(PageEntity pageEntity, List<LemmaEntity> lemmas) {
        return indexRepository.findByPageEntityAndLemmaEntityIn(pageEntity, lemmas)
                .stream()
                .map(e -> new LemmaData(e.getLemmaEntity().getLemma(), e.getRank()))
                .toList();
    }
}
