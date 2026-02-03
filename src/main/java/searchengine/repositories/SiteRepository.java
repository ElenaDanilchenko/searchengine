package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.SiteEntity;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    void deleteByUrl(String url);

    Optional<SiteEntity> findByUrl(String url);
}
