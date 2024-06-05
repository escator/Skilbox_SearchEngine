package searchengine.services;

import searchengine.config.SiteDto;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;

import java.time.LocalDateTime;
import java.util.List;

public interface IndexService {
    void indexingAll();
    void indexingSite(SiteDto siteDto);
    void delete(Site site);
    void deleteAll();
    List<Site> findAll();
    Site find(Site site);
    void updateDate(Site site, LocalDateTime date);
    void updateStatus(Site site, IndexingStatus indexingStatus);

}
