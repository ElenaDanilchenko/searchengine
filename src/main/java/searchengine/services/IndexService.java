package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.Model.IndexEntity;
import searchengine.Model.LemmaEntity;
import searchengine.Model.PageEntity;
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
}
