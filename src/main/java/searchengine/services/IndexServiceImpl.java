package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import searchengine.config.JsopConnectionCfg;
import searchengine.response.HtmlParseResponse;
import searchengine.dto.index.SiteDto;
import searchengine.config.SitesList;
import searchengine.dto.index.PageDto;
import searchengine.response.IndexingResponse;
import searchengine.model.*;
import searchengine.repository.IndexEntityRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.util.LinkToolsBox;
import searchengine.util.SiteToolsBox;

import java.io.IOException;
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
    private final LemmaRepository lemmaRepository;
    private final IndexEntityRepository indexEntityRepository;
    //TODO привести  интерфейс MorphologyService
    private final JsopConnectionCfg jsopConnectionCfg;

    @Override
    public IndexingResponse indexingAll() {
        log.info("Starting indexing all sites");
        clearDB();
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
        if ((s = findSite(null, siteDto.getName(), siteDto.getUrl())) != null) {
            deleteSite(s);
        }

        Site site = SiteToolsBox.siteDtoToSiteModel(siteDto);
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

    private void clearDB() {
        indexEntityRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();
    }

    @Override
    public IndexingResponse indexingPage(SiteDto siteDto) {
        String url = siteDto.getUrl().strip();
        if (!isValidSite(siteDto)) {
            log.info("Site {} is not valid", siteDto.getUrl());
            return new IndexingResponse(false, "Данная страница находится за пределами сайтов,указанных в конфигурационном файле");
        }

        // Если посещали данную страницу, то удаляем её из БД
        if (isVisitedLinks(url)) {
            deletePageByUrl(url);
        }

        HtmlParseService htmlParseService = new HtmlParseService(url, LinkToolsBox.extractRootDomain(url));
        HtmlParseResponse htmlParseResponse = htmlParseService.parse();
        if (htmlParseResponse.getStatus() == 200) {
            Page page = new Page();
            page.setPath(LinkToolsBox.getShortUrl(url, LinkToolsBox.extractRootDomain(url)));
            page.setCode(htmlParseResponse.getStatus());
            page.setSite(findSite(null, null, LinkToolsBox.extractRootDomain(url)));
            if (page.getSite() == null) {
                Site site = new Site();
                List<SiteDto> siteDtoList = sitesList.getSites();
                for (SiteDto siteD : siteDtoList) {
                    if (siteD.getUrl().equals(LinkToolsBox.extractRootDomain(url))) {
                        site.setName(siteD.getName());
                        site.setUrl(siteD.getUrl());
                        break;
                    }
                }
                site.setStatus(IndexingStatus.RANDOM_PAGE);
                site.setStatusTime(LocalDateTime.now());
                page.setSite(saveSite(site));
            }
            page.setContent(htmlParseResponse.getDocument().toString());
            page = savePage(page);

            lemmatizePage(page);
        }

        return new IndexingResponse(true, null);
    }

    private void lemmatizePage(Page page) {
        try {
            MorphologyService morphologyService = new MorphologyServiceImpl(this);
            morphologyService.processOnePage(this, page);
        } catch (IOException e) {
            log.error("Ошибка при инициализации MorphologyService", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Site saveSite(Site site) {
        return siteRepository.save(site);
    }

    @Override
    public void deleteSite(Site site) {
        List<Page> pages  = findPagesBySite(SiteToolsBox.siteModelToSiteDto(site));
        pages.forEach(this::deleteLemmaByPage);
        siteRepository.delete(site);
    }

    @Override
    public List<Site> findAll() {
        return siteRepository.findAll();
    }

    @Override
    public void deletePage(Page page) {
        deleteLemmaByPage(page);
        pageRepository.delete(page);
    }

    @Override
    public synchronized Page savePage(Page page) {
        return pageRepository.save(page);
    }


// TABLE Sites service

    /**
     * Поиск в БД таблица sites. Если указано id, то поиск будет произведен по id
     * параметры name и url будут проигнорированы.
     * если параметр id не указан, то поиск будет произведен по url или name
     *
     * @param id   Integer id записи (может быть null)
     * @param name String название сайта (может быть null)
     * @param url  String url сайта (может быть null)
     * @return Site сущность или null
     */
    @Override
    public Site findSite(Integer id, String name, String url) {
        if (id != null) {
            return findSiteById(id);
        }
        Site site = new Site();
        site.setName(name);
        site.setUrl(url);
        Example<Site> example = Example.of(site);
        return siteRepository.findOne(example).orElse(null);
    }

    @Override
    public Site findSiteById(Integer id) {
        return siteRepository.findById(id).orElse(null);
    }

    @Override
    public void updateDate(Site site, LocalDateTime date) {
        Site existingSite = findSite(null, site.getName(), site.getUrl());
        if (existingSite != null) {
            existingSite.setStatusTime(date);
            siteRepository.save(existingSite);
        }
    }

    @Override
    public void updateLastError(Site site, String error) {
        Site existingSite = findSite(null, site.getName(), site.getUrl());
        if (existingSite != null) {
            existingSite.setLastError(error);
            siteRepository.save(existingSite);
        }
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

    // TABLE Pages service
    @Override
    public int getPagesCount(SiteDto siteDto) {
        if (siteDto == null) {
            return (int) pageRepository.count();
        }
        Site site = findSite(null, siteDto.getName(), siteDto.getUrl());
        Page page = new Page();
        page.setSite(site);
        Example<Page> example = Example.of(page);
        Long count = pageRepository.count(example);
        return count.intValue();
    }

    @Override
    public List<Page> findPagesBySite(SiteDto siteDto) {
        Site site = findSite(null, siteDto.getName(), siteDto.getUrl());
        Page page = new Page();
        page.setSite(site);
        Example<Page> example = Example.of(page);
        List<Page> result = pageRepository.findAll(example);
        return result;
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

    // TABLE Lemma service
    private void deleteLemmaByPage(Page page) {
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
    public Integer lemmaCount(Example<Lemma> example) {
        int res = 0;
        if (example  == null) {
            res = (int)lemmaRepository.count();
        } else {
            res = (int)lemmaRepository.count(example);
        }
        return res;
    }

    /**
     * Проверяем присутствует ли в списке сайтов для индексации данный домен
     *
     * @param siteDto
     * @return
     */
    public boolean isValidSite(SiteDto siteDto) {
        String url = siteDto.getUrl().strip();
        boolean res = false;
        if (url == null || url.isEmpty()) {
            res = false;
        }
        List<SiteDto> siteList = sitesList.getSites();
        for (SiteDto siteCfg : siteList) {
            if (url.startsWith(siteCfg.getUrl())) {
                res = true;
                break;
            }
        }
        return res;
    }

    @Override
    public boolean isVisitedLinks(String url) {
        Page page = new Page();
        page.setPath(LinkToolsBox.getShortUrl(url));
        page.setSite(findSite(null, null, url));
        Example<Page> example = Example.of(page);
        return pageRepository.exists(example);
    }

    //TODO Удалить перед сдачей проекта
    public void test() {
        log.info("test");
        List<SiteDto> siteList = sitesList.getSites();
        log.info("{}, {}", siteList.get(0).getUrl(), getPagesCount(siteList.get(0)));
    }
}
