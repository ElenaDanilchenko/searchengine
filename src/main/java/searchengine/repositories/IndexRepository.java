package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    @Query("SELECT i.lemmaEntity.id FROM IndexEntity i WHERE i.pageEntity = :pageEntity")
    List<Integer> findLemmasIdsByPageEntity(PageEntity pageEntity);

    List<IndexEntity> findByLemmaEntity(LemmaEntity lemmaEntity);

    List<IndexEntity> findByPageEntityInAndLemmaEntity(List<PageEntity> pages, LemmaEntity lemmaEntity);

    List<IndexEntity> findByPageEntityAndLemmaEntityIn(PageEntity pageEntity, List<LemmaEntity> lemmas);
}
