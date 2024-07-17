package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import searchengine.dto.index.SiteDto;
import searchengine.model.IndexEntity;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexEntityRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.util.LinkToolsBox;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {

    private final SiteService siteService;
    private final PageRepository pageRepository;
    private final IndexEntityRepository indexEntityRepository;
    private final LemmaRepository lemmaRepository;
    @Override
    public List<Page> findPagesBySite(SiteDto siteDto) {
        Site site = siteService.findSite(null, siteDto.getName(), siteDto.getUrl());
        Page page = new Page();
        page.setSite(site);
        Example<Page> example = Example.of(page);
        List<Page> result = pageRepository.findAll(example);
        return result;
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
        page.setSite(siteService.findSite(null, null, LinkToolsBox.extractRootDomain(url)));
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
    public int getPagesCount(SiteDto siteDto) {
        if (siteDto == null) {
            return (int) pageRepository.count();
        }
        Site site = siteService.findSite(null, siteDto.getName(), siteDto.getUrl());
        Page page = new Page();
        page.setSite(site);
        Example<Page> example = Example.of(page);
        Long count = pageRepository.count(example);
        return count.intValue();
    }
}
