package searchengine.services;

import searchengine.dto.index.SiteDto;
import searchengine.dto.statistics.IndexingResponse;
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
    Site saveSite(Site site);
    void deleteSite(Site site);
    List<Site> findAll();
    Site findSite(Integer id, String name, String url);
    Site findSiteById(Integer id);
    void updateDate(Site site, LocalDateTime date);
    void updateStatus(Site site, IndexingStatus indexingStatus);
    void updateLastError(Site site, String error);
    int getPagesCount(SiteDto siteDto);
    void savePage(Page page);
    void deletePage(Page page);
    boolean isVisitedLinks(String url);
    public List<Page> findPagesBySite(SiteDto siteDto);
    LemmaRepository getLemmaRepository();
    IndexEntityRepository getIndexEntityRepository();
}
