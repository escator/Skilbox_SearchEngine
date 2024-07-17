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
    private final PageService pageService;
    private final SiteService siteService;
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
        if ((s = siteService.findSite(null, siteDto.getName(), siteDto.getUrl())) != null) {
            siteService.deleteSite(s);
        }

        Site site = SiteToolsBox.siteDtoToSiteModel(siteDto);
        site.setStatus(IndexingStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        site = siteService.saveSite(site);

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
            pageService.deletePageByUrl(url);
        }

        HtmlParseService htmlParseService = new HtmlParseService(url, LinkToolsBox.extractRootDomain(url));
        HtmlParseResponse htmlParseResponse = htmlParseService.parse();
        if (htmlParseResponse.getStatus() == 200) {
            Page page = new Page();
            page.setPath(LinkToolsBox.getShortUrl(url, LinkToolsBox.extractRootDomain(url)));
            page.setCode(htmlParseResponse.getStatus());
            page.setSite(siteService.findSite(null, null, LinkToolsBox.extractRootDomain(url)));
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
                page.setSite(siteService.saveSite(site));
            }
            page.setContent(htmlParseResponse.getDocument().toString());
            page = pageService.savePage(page);

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
    public void updateDate(Site site, LocalDateTime date) {
        Site existingSite = siteService.findSite(null, site.getName(), site.getUrl());
        if (existingSite != null) {
            existingSite.setStatusTime(date);
            siteRepository.save(existingSite);
        }
    }

    @Override
    public void updateLastError(Site site, String error) {
        Site existingSite = siteService.findSite(null, site.getName(), site.getUrl());
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


    // TABLE Lemma service


    @Override
    public Integer lemmaCount(Example<Lemma> example) {
        int res = 0;
        if (example == null) {
            res = (int) lemmaRepository.count();
        } else {
            res = (int) lemmaRepository.count(example);
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
        page.setSite(siteService.findSite(null, null, url));
        Example<Page> example = Example.of(page);
        return pageRepository.exists(example);
    }

    //TODO Удалить перед сдачей проекта
    public void test() {
        log.info("test");

    }
}
