package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.Model.IndexEntity;
import searchengine.Model.PageEntity;

import java.util.List;

public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    @Query("SELECT i.lemmaEntity.id FROM IndexEntity i WHERE i.pageEntity = :pageEntity")
    List<Integer> findLemmasIdsByPageEntity(PageEntity pageEntity);
}
