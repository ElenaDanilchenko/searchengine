package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.Model.SiteEntity;
import searchengine.Model.Status;
import searchengine.config.Site;
import searchengine.repositories.SiteRepository;

@RequiredArgsConstructor
@Service
public class SiteService {

    private final SiteRepository siteRepository;

    @Transactional
    public SiteEntity create(Site site) {
        SiteEntity siteEntity = SiteEntity.builder()
                .url(site.getUrl())
                .name(site.getName())
                .status(Status.INDEXING)
                .build();
        siteRepository.save(siteEntity);
        return siteEntity;
    }

    @Transactional
    public void clearTables(Site site) {
        siteRepository.deleteByUrl(site.getUrl());
    }

    public void markFailed(SiteEntity siteEntity, String message) {
        siteEntity.setStatus(Status.FAILED);
        siteEntity.setLastError(message);
        siteRepository.save(siteEntity);
    }

    public void markIndexed(SiteEntity siteEntity) {
        siteEntity.setStatus(Status.INDEXED);
        siteRepository.save(siteEntity);
    }

    public void updateStatusTime(SiteEntity siteEntity) {
        siteRepository.save(siteEntity);
    }
}
