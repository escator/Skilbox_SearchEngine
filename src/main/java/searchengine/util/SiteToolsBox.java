package searchengine.util;

import searchengine.dto.index.SiteDto;
import searchengine.model.Site;

public class SiteToolsBox {

    public static Site siteDtoToSiteModel(SiteDto siteCfg) {
        if (siteCfg == null) {
            return null;
        }
        searchengine.model.Site site = new searchengine.model.Site();
        site.setName(siteCfg.getName());
        site.setUrl(siteCfg.getUrl());
        return site;
    }

    public static SiteDto siteModelToSiteDto(searchengine.model.Site site)  {
        if (site == null) {
            return null;
        }
        SiteDto siteDto = new SiteDto();
        siteDto.setName(site.getName());
        siteDto.setUrl(site.getUrl());
        return siteDto;
    }
}
