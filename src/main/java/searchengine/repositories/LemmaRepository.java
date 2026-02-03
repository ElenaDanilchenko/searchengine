package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE LemmaEntity l SET l.frequency = l.frequency - 1 WHERE l.id in :ids")
    void decrementFrequencyByIds(List<Integer> ids);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM LemmaEntity l WHERE l.frequency = 0")
    void deleteLemmasWithZeroFrequency();

    List<LemmaEntity> findByLemmaInAndSiteEntity(List<String> lemmas, SiteEntity siteEntity);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO lemma (site_id, lemma, frequency) 
            VALUES (:siteId, :lemma, 1) 
            ON DUPLICATE KEY UPDATE frequency=frequency+1""",
            nativeQuery = true)
    void upsert(String lemma, int siteId);

    int countBySiteEntity(SiteEntity siteEntity);
}
