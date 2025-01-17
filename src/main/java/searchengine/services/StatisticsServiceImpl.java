package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.index.SiteDto;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.response.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final Random random = new Random();
    private final SitesList sites;
    private final IndexService indexService;
    private final SiteService siteService;

    @Override
    public StatisticsResponse getStatistics() {
        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);
        total.setPages(0);
        //total.setSites(0);
        total.setLemmas(0);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteDto> sitesList = sites.getSites();
        for(int i = 0; i < sitesList.size(); i++) {
            SiteDto site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            // получаем количество проиндексированных страниц на сайте
            int pages = siteService.countPagesOnSite(sitesList.get(i));

            int lemmas = siteService.сountAllLemmasOnSite(sitesList.get(i));
            item.setPages(pages);
            item.setLemmas(lemmas);

            Site siteEntity = siteService.findSite(null, sitesList.get(i).getName(), sitesList.get(i).getUrl());
            if (siteEntity == null) {
                break;
            }
            item.setStatus(siteEntity.getStatus().name());
            String error = siteEntity.getLastError();
            item.setError((error == null) ? "null" : error);
            item.setStatusTime(siteService.getStatusTime(sitesList.get(i)));


            //total.setSites();
            total.setPages(siteService.countPagesOnSite(null));
            total.setLemmas(siteService.сountAllLemmasOnSite(null));
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }


}
