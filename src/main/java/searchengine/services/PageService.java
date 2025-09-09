package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.Model.PageEntity;
import searchengine.Model.SiteEntity;
import searchengine.repositories.PageRepository;

@RequiredArgsConstructor
@Service
public class PageService {

    private final PageRepository pageRepository;

    @Transactional
    public void create(PageEntity pageEntity) {
        pageRepository.save(pageEntity);

    }

    @Transactional(readOnly = true)
    public boolean isNewPage(SiteEntity siteEntity, String path) {
        return pageRepository.findBySiteEntityAndPath(siteEntity, path).isEmpty();
    }
}
