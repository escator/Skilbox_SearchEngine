package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import searchengine.dto.index.SiteDto;
import searchengine.model.*;
import searchengine.repository.IndexEntityRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.util.LinkToolsBox;
import searchengine.util.SiteToolsBox;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexEntityRepository indexEntityRepository;
    private final LemmaRepository lemmaRepository;
    //private final PageService pageService;


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

    public Site findSiteByDTO(SiteDto siteDto) {
        if (siteDto == null) {
            return null;
        }
        return findSite(null, siteDto.getName(), siteDto.getUrl());
    }

    @Override
    public Site saveSite(Site site) {
        return siteRepository.save(site);
    }

    @Override
    public void deleteSite(Site site) {
        List<Page> pages = findPagesBySite(SiteToolsBox.siteModelToSiteDto(site));
        pages.forEach(this::deleteLemmaByPage);
        siteRepository.delete(site);
    }

    private Site findSiteById(Integer id) {
        return siteRepository.findById(id).orElse(null);
    }

    @Override
    public void updateLastErrorOnSite(Site site, String error) {
        Site existingSite = findSite(null, site.getName(), site.getUrl());
        if (existingSite != null) {
            existingSite.setLastError(error);
            saveSite(existingSite);
        }
    }

    @Override
    public void updateStatusOnSite(Site site, IndexingStatus newIndexingStatus) {
        log.info("Updating site STATUS {} on {}", site.getUrl(), site.getStatus());
        Site existingSite = findSiteById(site.getId());
        if (existingSite != null) {
            existingSite.setStatus(newIndexingStatus);
            existingSite.setStatusTime(LocalDateTime.now());
            saveSite(existingSite);
        }
    }

    @Override
    public List<Page> findPagesBySite(SiteDto siteDto) {
        Site site;
        if ((site = findSiteByDTO(siteDto)) == null) {
            return new ArrayList<>();
        } else {
            Page page = new Page();
            page.setSite(site);
            Example<Page> example = Example.of(page);
            List<Page> result = pageRepository.findAll(example);
            return result;
        }
    }

    @Override
    public synchronized Page savePage(Page page) {
        return pageRepository.save(page);
    }

    @Override
    public void deletePage(Page page) {
        deleteLemmaByPage(page);
        pageRepository.delete(page);
    }

    public void deletePageByUrl(String url) {
        Page page = new Page();
        page.setPath(LinkToolsBox.getShortUrl(url, LinkToolsBox.extractRootDomain(url)));
        page.setSite(findSite(null, null, LinkToolsBox.extractRootDomain(url)));
        Example<Page> example = Example.of(page);
        Optional<Page> pageOptional = pageRepository.findOne(example);
        if (pageOptional.isPresent()) {
            deleteLemmaByPage(pageOptional.get());
            deletePage(pageOptional.get());
        }
    }

    public void deleteLemmaByPage(Page page) {
        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setPage(page);
        Example<IndexEntity> example = Example.of(indexEntity);
        List<IndexEntity> indexEntities = indexEntityRepository.findAll(example);
        for (IndexEntity indexEn : indexEntities) {
            Lemma lemma = indexEn.getLemma();
            lemma.decrementFrequency();
            lemmaRepository.save(lemma);
        }
        indexEntityRepository.deleteAll(indexEntities);
    }

    @Override
    public int countPagesOnSite(SiteDto siteDto) {
        int res = 0;
        Site site;
        if ((site = findSiteByDTO(siteDto)) == null) {
            res = (int) pageRepository.count();
        } else {
            Page page = new Page();
            page.setSite(site);
            Example<Page> example = Example.of(page);
            Long count = pageRepository.count(example);
            res = count.intValue();
        }
        return res;
    }


    @Override
    public int сountAllLemmasOnSite(SiteDto siteDto) {
        int res = 0;
        Site site;
        if ((site = findSiteByDTO(siteDto)) == null) {
            res = (int) lemmaRepository.count();
        } else {
            Lemma lemma = new Lemma();
            lemma.setSite(site);
            res = (int) lemmaRepository.count(Example.of(lemma));
        }
        return res;
    }

    @Override
    public void deleteAllSite() {
        try {
            indexEntityRepository.deleteAll();
            lemmaRepository.deleteAll();
            pageRepository.deleteAll();
            siteRepository.deleteAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getStatusTime(SiteDto siteDto) {
        long res = 0;
        Site site;
        if ((site = findSiteByDTO(siteDto)) == null) {
            return res;
        } else {
            LocalDateTime statusTime = site.getStatusTime();
            res = statusTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        return res;
    }


}
