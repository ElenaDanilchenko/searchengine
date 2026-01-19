package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.Model.PageEntity;
import searchengine.Model.SiteEntity;
import searchengine.dto.indexing.PageData;
import searchengine.repositories.PageRepository;
import searchengine.utils.UrlUtils;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class PageService {

    private final PageRepository pageRepository;
    private final IndexService indexService;
    private final LemmaService lemmaService;

    @Transactional
    public PageEntity create(SiteEntity siteEntity, PageData pageData) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setSiteEntity(siteEntity);
        pageEntity.setPath(UrlUtils.getRelativePath(pageData.fullPath()));
        pageEntity.setCode(pageData.statusCode());
        pageEntity.setContent(pageData.content());
        return pageRepository.saveAndFlush(pageEntity);
    }

    @Transactional(readOnly = true)
    public boolean isNewPage(SiteEntity siteEntity, String fullPath) {
        return pageRepository.findBySiteEntityAndPath(siteEntity, UrlUtils.getRelativePath(fullPath)).isEmpty();
    }

    @Transactional(readOnly = true)
    public Optional<PageEntity> findPage(SiteEntity siteEntity, String fullPath) {
        return pageRepository.findBySiteEntityAndPath(siteEntity, UrlUtils.getRelativePath(fullPath));
    }

    @Transactional
    public void deletePage(PageEntity pageEntity) {
        List<Integer> lemmasIds = indexService.getLemmasIdsOnPage(pageEntity);
        if (!lemmasIds.isEmpty()) {
            lemmaService.decrementFrequency(lemmasIds);
            lemmaService.deleteLemmasWithZeroFrequency();
        }
        pageRepository.delete(pageEntity);
    }

    @Transactional(readOnly = true)
    public int getPagesCountOnSite(SiteEntity siteEntity) {
        return pageRepository.countBySiteEntity(siteEntity);
    }
}
