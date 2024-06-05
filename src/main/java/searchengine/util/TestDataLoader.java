package searchengine.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.config.SiteDto;
import searchengine.config.SitesList;
import searchengine.model.IndexingStatus;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TestDataLoader {
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;

    public void saveSiteToDB(SiteDto siteCfgItem) {
        Site site = new Site();
        site.setName(siteCfgItem.getName());
        site.setUrl(siteCfgItem.getUrl());
        site.setStatus(IndexingStatus.INDEXED);
        site.setStatusTime(LocalDateTime.now());
        Page page1 = new Page();
        page1.setContent("Test Page 1");
        page1.setCode(400);
        page1.setSite(site);
        page1.setPath(siteCfgItem.getUrl() + "/page1");
        Page page2 = new Page();
        page2.setContent("Test Page 2");
        page2.setCode(401);
        page2.setSite(site);
        page2.setPath(siteCfgItem.getUrl() + "/page2");

        site.setPages(List.of(page1,page2));
        siteRepository.save(site);
    }

    public void loadTestSites()  {
        List<SiteDto> sites = sitesList.getSites();

        for (SiteDto siteCfgItem : sites) {
            saveSiteToDB(siteCfgItem);
        }
    }
}
