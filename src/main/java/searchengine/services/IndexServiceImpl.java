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
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Getter
public class IndexServiceImpl implements IndexService {

    // get sites list
    private final SitesList sitesList;
    private final SiteService siteService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexEntityRepository indexEntityRepository;
    private final JsopConnectionCfg jsopConnectionCfg;

    @Override
    public IndexingResponse indexingAllSites() {
        log.info("Starting indexing all sites");
        siteService.deleteAllSite();
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

        // Если данный сайт уже проиндексированы, то удаляем все
        // данные о нем из БД
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

    @Override
    public IndexingResponse indexingPage(SiteDto siteDto) {
        String url = siteDto.getUrl().strip();
        if (!isValidSite(siteDto)) {
            log.info("Site {} is not valid", siteDto.getUrl());
            return new IndexingResponse(false, "Данная страница находится за пределами сайтов,указанных в конфигурационном файле");
        }

        int code = LinkToolsBox.checkConnectLink(url);
        if (!jsopConnectionCfg.getValidCodes().contains(code)) {
            return new IndexingResponse(false, "Код: " + code + " Ошибка доступа к " + url);
        }

        // Если посещали данную страницу, то удаляем её из БД
        if (isVisitedLinks(url)) {
            siteService.deletePageByUrl(url);
        }

        HtmlParseResponse htmlParseResponse = new HtmlParseService(url, LinkToolsBox.extractRootDomain(url)).parse();
        if (jsopConnectionCfg.getValidCodes().contains(htmlParseResponse.getStatus())) {
            Page page = new Page();
            page.setPath(LinkToolsBox.getShortUrl(url, LinkToolsBox.extractRootDomain(url)));
            page.setCode(htmlParseResponse.getStatus());
            page.setSite(siteService.findSite(null, null, LinkToolsBox.extractRootDomain(url)));

            page.setContent(htmlParseResponse.getDocument().toString());
            page = siteService.savePage(page);

            lemmatizePage(page);
        }

        return new IndexingResponse(true, null);
    }




    private void lemmatizePage(Page page) {

        try {
            MorphologyService morphologyService = new MorphologyServiceImpl();
            morphologyService.processOnePage(page);
        } catch (IOException e) {
            log.error("Ошибка при инициализации MorphologyService", e);
            throw new RuntimeException(e);
        }
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

}
