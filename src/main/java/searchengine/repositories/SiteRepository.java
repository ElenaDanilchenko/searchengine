package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.Model.SiteEntity;

public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    void deleteByUrl(String url);
}
