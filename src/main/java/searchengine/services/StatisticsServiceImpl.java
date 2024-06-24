package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.index.SiteDto;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
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

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteDto> sitesList = sites.getSites();
        for(int i = 0; i < sitesList.size(); i++) {
            SiteDto site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            // получаем количество проиндексированных страниц на сайте
            int pages = indexService.getPagesCount(sitesList.get(i));

            int lemmas = indexService.lemmaCount();
            item.setPages(pages);
            item.setLemmas(lemmas);

            // TODO установить нулевые данные при первом запуске
            Site siteEntity = indexService.findSite(null, sitesList.get(i).getName(), sitesList.get(i).getUrl());
            item.setStatus(siteEntity.getStatus().name());
            String error = siteEntity.getLastError();
            item.setError((error == null) ? "null" : error);
            item.setStatusTime(System.currentTimeMillis() -
                    (random.nextInt(10_000)));


            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
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
