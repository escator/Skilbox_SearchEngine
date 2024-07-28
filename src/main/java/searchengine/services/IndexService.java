package searchengine.services;

import searchengine.dto.index.SiteDto;
import searchengine.response.IndexingResponse;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;
import searchengine.repository.IndexEntityRepository;
import searchengine.repository.LemmaRepository;

import java.time.LocalDateTime;

public interface IndexService {
    IndexingResponse indexingAllSites();
    void indexingSite(SiteDto siteDto);
    IndexingResponse indexingPage(SiteDto siteDto);
    boolean isVisitedLinks(String url);
    LemmaRepository getLemmaRepository();
    IndexEntityRepository getIndexEntityRepository();
    SiteService getSiteService();

}
