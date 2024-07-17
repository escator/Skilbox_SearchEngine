package searchengine.services;

import org.springframework.data.domain.Example;
import searchengine.dto.index.SiteDto;
import searchengine.model.Lemma;
import searchengine.response.IndexingResponse;
import searchengine.model.IndexingStatus;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexEntityRepository;
import searchengine.repository.LemmaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface IndexService {
    IndexingResponse indexingAll();
    void indexingSite(SiteDto siteDto);
    IndexingResponse indexingPage(SiteDto siteDto);

    void updateDate(Site site, LocalDateTime date);
    void updateStatus(Site site, IndexingStatus indexingStatus);
    void updateLastError(Site site, String error);
    boolean isVisitedLinks(String url);
    LemmaRepository getLemmaRepository();
    IndexEntityRepository getIndexEntityRepository();
    PageService getPageService();
    Integer lemmaCount(Example<Lemma> example);
}
