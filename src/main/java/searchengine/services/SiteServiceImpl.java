package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.SiteRepository;
import searchengine.util.SiteToolsBox;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {
    private final SiteRepository siteRepository;
    private final PageService pageService;

    @Override
    public Site findSite(Integer id, String name, String url) {
        Site res = null;
        if (id != null) {
            return findSiteById(id);
        } else if (name != null || url != null) {
            Site site = new Site();
            site.setName(name);
            site.setUrl(url);
            Example<Site> example = Example.of(site);
            Optional<Site> optionalSite = siteRepository.findOne(example);
            if (!optionalSite.isEmpty()) {
                res = optionalSite.get();
            }
        }
        return res; // return null if not found
    }

    @Override
    public Site saveSite(Site site) {
        return siteRepository.save(site);
    }

    @Override
    public void deleteSite(Site site) {
        List<Page> pages = pageService.findPagesBySite(SiteToolsBox.siteModelToSiteDto(site));
        pages.forEach(pageService::deleteLemmaByPage);
        siteRepository.delete(site);
    }

    @Override
    public Site findSiteById(Integer id) {
        return siteRepository.findById(id).orElse(null);
    }
    @Override
    public List<Site> findAllSites() {
        return siteRepository.findAll();
    }


}
