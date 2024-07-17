package searchengine.services;

import searchengine.dto.index.SiteDto;
import searchengine.model.Page;

import java.util.List;

public interface PageService {
    public List<Page> findPagesBySite(SiteDto siteDto);
    Page savePage(Page page);
    void deletePage(Page page);
    void deletePageByUrl(String url);
    void deleteLemmaByPage(Page page);
    int getPagesCount(SiteDto siteDto);

}
