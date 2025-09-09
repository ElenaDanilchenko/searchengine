package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.Model.PageEntity;
import searchengine.Model.SiteEntity;

import java.util.Optional;

public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    Optional<PageEntity> findBySiteEntityAndPath(SiteEntity siteEntity, String path);
}

