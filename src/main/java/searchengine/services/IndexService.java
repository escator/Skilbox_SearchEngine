package searchengine.services;

import searchengine.dto.index.SiteDto;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;

import java.time.LocalDateTime;
import java.util.List;

public interface IndexService {
    IndexingResponse indexingAll();
    void indexingSite(SiteDto siteDto);
    void deleteSite(Site site);
    void deleteAll();
    List<Site> findAll();
    Site find(Integer id, String name, String url);
    Site findById(Integer id);
    void updateDate(Site site, LocalDateTime date);
    void updateStatus(Site site, IndexingStatus indexingStatus);
    void updateLastError(Site site, String error);
    int getPagesCount(SiteDto siteDto);
}
