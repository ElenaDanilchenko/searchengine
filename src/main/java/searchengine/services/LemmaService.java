package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.Model.LemmaEntity;
import searchengine.Model.SiteEntity;
import searchengine.repositories.LemmaRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LemmaService {

    private final LemmaRepository lemmaRepository;

    @Transactional
    @Retryable(retryFor = CannotAcquireLockException.class, maxAttempts = 10, backoff = @Backoff(delay = 100, multiplier = 2))
    public void merge(Set<String> lemmas, SiteEntity siteEntity) {
        List<String> sortedLemmas = new ArrayList<>(lemmas);
        Collections.sort(sortedLemmas);
        for (String lemma : sortedLemmas) {
            lemmaRepository.upsert(lemma, siteEntity.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<LemmaEntity> getLemmasEntitiesOnSite(Set<String> lemmas, SiteEntity siteEntity) {
        return lemmaRepository.findByLemmaInAndSiteEntity(new ArrayList<>(lemmas), siteEntity);
    }

    public void decrementFrequency(List<Integer> lemmasIds) {
        lemmaRepository.decrementFrequencyByIds(lemmasIds);
    }

    public void deleteLemmasWithZeroFrequency() {
        lemmaRepository.deleteLemmasWithZeroFrequency();
    }

    @Transactional(readOnly = true)
    public int getLemmasCountOnSite(SiteEntity siteEntity) {
        return lemmaRepository.countBySiteEntity(siteEntity);
    }
}
