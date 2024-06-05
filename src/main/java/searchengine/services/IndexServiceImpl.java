package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import searchengine.config.JsopConnectionCfg;
import searchengine.config.SiteDto;
import searchengine.config.SitesList;
import searchengine.dto.index.PageDto;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    // get sites list
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final JsopConnectionCfg jsopConnectionCfg;

    @Override
    public void indexingAll() {
        log.info("Starting indexing");
        List<SiteDto> siteList = sitesList.getSites();
        for (SiteDto site : siteList) {
            log.info(site.toString());
            indexingSite(site);
        }

    }

    @Override
    public void indexingSite(SiteDto siteDto)  {
        Site site  = new Site();
        site.setName(siteDto.getName());
        site.setUrl(siteDto.getUrl());
        site.setStatus(IndexingStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        site = save(site);


        PageDto pageDto = new PageDto(
                siteDto.getUrl(),
                siteDto.getUrl(),
                site,
                pageRepository);

        new ForkJoinPool().invoke(new PageScannerService(pageDto));
    }

    public Site save(Site site)  {
        return siteRepository.save(site);
    }
    @Override
    public void delete(Site site) {
        siteRepository.delete(site);
    }

    @Override
    public List<Site> findAll() {
        return siteRepository.findAll();
    }

    @Override
    public void deleteAll() {
        log.info("Deleting all sites");
        siteRepository.deleteAll();
    }

    @Override
    public Site find(Site site) {
        Example<Site> example = Example.of(site);
        return siteRepository.findOne(example).orElse(null);
    }

    @Override
    public void updateDate(Site site, LocalDateTime date) {
        Site existingSite = find(site);
        existingSite.setStatusTime(date);
        siteRepository.save(existingSite);
    }

    @Override
    public void updateStatus(Site site, IndexingStatus indexingStatus) {
        Site existingSite = find(site);
        existingSite.setStatus(indexingStatus);
        siteRepository.save(existingSite);
    }

    public static searchengine.model.Site siteCfgToSiteModel(SiteDto siteCfg) {
        searchengine.model.Site site = new searchengine.model.Site();
        site.setName(siteCfg.getName());
        site.setUrl(siteCfg.getUrl());
        return site;
    }
}
