package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import searchengine.config.JsopConnectionCfg;
import searchengine.dto.index.HtmlParseResponse;
import searchengine.dto.index.SiteDto;
import searchengine.config.SitesList;
import searchengine.dto.index.PageDto;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.model.IndexingStatus;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.util.LinkToolsBox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Getter
public class IndexServiceImpl implements IndexService {

    // get sites list
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final JsopConnectionCfg jsopConnectionCfg;

    @Override
    public IndexingResponse indexingAll() {
        log.info("Starting indexing");
        RunIndexMonitor.setIndexingRunning(true);
        List<SiteDto> siteList = sitesList.getSites();
        for (SiteDto siteDto : siteList) {
            log.info(siteDto.toString());
            indexingSite(siteDto);
        }
        return new IndexingResponse(true, null);
    }

    @Override
    public void indexingSite(SiteDto siteDto) {
        if (!isValidSite(siteDto)) {
            log.info("Site is not valid");
            return;
        }
        Site s;
        if ((s = find(null, siteDto.getName(), siteDto.getUrl())) != null)  {
            deleteSite(s);
        }

        Site site  = siteDtoToSiteModel(siteDto);
        site.setStatus(IndexingStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        site = saveSite(site);

        PageDto pageDto = new PageDto(
                siteDto.getUrl(),
                siteDto.getUrl(),
                site);

        Thread thread = new Thread(new ThreadIndexingManager(pageDto, this));
        thread.start();
    }

    public boolean isVisitedLinks(String url) {
        Page page = new Page();
        page.setPath(LinkToolsBox.getShortUrl(url));
        page.setSite(find(null, null, url));
        Example<Page> example  = Example.of(page);
        return pageRepository.exists(example);
    }

    public IndexingResponse indexingPage(SiteDto siteDto) {
        String url = siteDto.getUrl().strip();
        if (!isValidSite(siteDto)) {
            return new IndexingResponse(false, "Данная страница находится за пределами сайтов,указанных в конфигурационном файле");
        }

        // Если посещали данную страницу, то удаляем её из БД
        if (isVisitedLinks(url))   {
            Page page  = new Page();
            page.setPath(LinkToolsBox.getShortUrl(url, LinkToolsBox.extractRootDomain(url)));
            page.setSite(find(null, null, LinkToolsBox.extractRootDomain(url)));
            Example<Page> example = Example.of(page);
            Optional<Page> pageOptional = pageRepository.findOne(example);
            if (pageOptional.isPresent())    {
                deletePage(pageOptional.get());
            }
        }
        HtmlParseService htmlParseService   = new HtmlParseService(url, LinkToolsBox.extractRootDomain(url));
        HtmlParseResponse htmlParseResponse  = htmlParseService.parse();
        if (htmlParseResponse.getStatus() == 200) {
            Page page = new Page();
            page.setPath(LinkToolsBox.getShortUrl(url, LinkToolsBox.extractRootDomain(url)));
            page.setCode(htmlParseResponse.getStatus());
            page.setSite(find(null, null, LinkToolsBox.extractRootDomain(url)));
            page.setContent(htmlParseResponse.getDocument().toString());
            savePage(page);
        }



        return new IndexingResponse(true, null);
    }

    public Site saveSite(Site site)  {
        return siteRepository.save(site);
    }
    @Override
    public void deleteSite(Site site) {
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

    public void deletePage(Page page)  {
        pageRepository.delete(page);
    }
    public synchronized void savePage(Page page)   {pageRepository.save(page);}


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
    public  void updateLastError(Site site, String error)   {
        Site existingSite = find(null, site.getName(), site.getUrl());
        if (existingSite != null)  {
            existingSite.setLastError(error);
            siteRepository.save(existingSite);
        }
    }

    @Override
    public int getPagesCount(SiteDto siteDto)  {
        if (siteDto  == null) {
            return (int) pageRepository.count();
        }
        Site site = find(null, siteDto.getName(), siteDto.getUrl());
        Page page = new Page();
        page.setSite(site);
        Example<Page> example  = Example.of(page);
        Long count  = pageRepository.count(example);
        return count.intValue();
    }

    @Override
    public void updateStatus(Site site, IndexingStatus newIndexingStatus) {
        log.info("Updating site STATUS {} on {}", site.getUrl(), site.getStatus());
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
        String url  = siteDto.getUrl().strip();
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

    //TODO Удалить перед сдачей проекта
    public void test() {
        log.info("test");
        List<SiteDto> siteList  = sitesList.getSites();
        log.info("{}, {}",siteList.get(0).getUrl(), getPagesCount(siteList.get(0)));
    }
}
