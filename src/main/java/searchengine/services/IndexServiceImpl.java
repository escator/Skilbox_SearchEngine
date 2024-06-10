package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import searchengine.Application;
import searchengine.config.JsopConnectionCfg;
import searchengine.config.SiteDto;
import searchengine.config.SitesList;
import searchengine.dto.index.PageDto;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;
import searchengine.repository.LinkStorage;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    public IndexingResponse indexingAll() {
        log.info("Starting indexing");
        Application.setIndexingRunning(true);
        List<SiteDto> siteList = sitesList.getSites();
        for (SiteDto siteDto : siteList) {
            log.info(siteDto.toString());
            indexingSite(siteDto);
        }
        return new IndexingResponse(true, null);
    }

    @Override
    public void indexingSite(SiteDto siteDto)  {
        if (!isValidSite(siteDto)) {
            log.info("Site is not valid");
            return;
        }
        Application.setIndexingRunning(true);
        Site s;
        if ((s = find(null, siteDto.getName(), siteDto.getUrl())) != null)  {
            delete(s);
        }


        Site site  = siteDtoToSiteModel(siteDto);
        site.setStatus(IndexingStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        site = save(site);

        PageDto pageDto = new PageDto(
                siteDto.getUrl(),
                siteDto.getUrl(),
                site,
                pageRepository);

        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.invoke(new PageScannerService(pageDto));
        while (forkJoinPool.getQueuedSubmissionCount() > 0 ||
        forkJoinPool.getActiveThreadCount() > 0) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LinkStorage.removeAll();
        if (Application.isStopIndexing()) {
            updateStatus(site, IndexingStatus.FAILED);
        } else  {
            updateStatus(site, IndexingStatus.INDEXED);
        }
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
        log.info("Deleting all sites and all pages");
        siteRepository.deleteAll();
        pageRepository.deleteAll();
    }

    /**
     * Поиск в БД таблица sites. Если указано id, то поиск будет произведен по id
     * параметры name и url будут проигнорированы.
     * если параметр id не указан, то поиск будет произведен по url или name
     * @param id    Integer id записи (может быть null)
     * @param name  String название сайта (может быть null)
     * @param url   String url сайта (может быть null)
     * @return  Site сущность или null
     */
    @Override
    public Site find(Integer id, String name, String url) {
        if (id != null)  {
            return findById(id);
        }
        Site site = new Site();
        site.setName(name);
        site.setUrl(url);
        Example<Site> example = Example.of(site);
        return siteRepository.findOne(example).orElse(null);
    }

    @Override
    public Site findById(Integer id)  {
        return siteRepository.findById(id).orElse(null);
    }

    @Override
    public void updateDate(Site site, LocalDateTime date) {
        Site existingSite = find(null, site.getName(), site.getUrl());
        if (existingSite != null)  {
            existingSite.setStatusTime(date);
            siteRepository.save(existingSite);
        }
    }

    @Override
    public void updateStatus(Site site, IndexingStatus newIndexingStatus) {
        log.info("Updating site STATUS" + site.getName());
        Optional<Site> optionalSite = siteRepository.findById(site.getId());
        if (optionalSite.isPresent()) {
            Site existingSite = optionalSite.get();
            existingSite.setStatus(newIndexingStatus);
            existingSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(existingSite);
        }
    }

    /**
     * Проверяем присутствует ли в списке сайтов для индексации данный домен
     * @param siteDto
     * @return
     */
    public boolean isValidSite(SiteDto siteDto)   {
        String url  = siteDto.getUrl();
        boolean res = false;
        if (url == null || url.isEmpty()) {
            res = false;
        }
        List<SiteDto> siteList = sitesList.getSites();
        for  (SiteDto siteCfg : siteList) {
            if (url.startsWith(siteCfg.getUrl())) {
                res = true;
                break;
            }
        }
        return res;
    }
    public static searchengine.model.Site siteDtoToSiteModel(SiteDto siteCfg) {
        searchengine.model.Site site = new searchengine.model.Site();
        site.setName(siteCfg.getName());
        site.setUrl(siteCfg.getUrl());
        return site;
    }
}
