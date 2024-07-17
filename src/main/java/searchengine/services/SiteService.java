package searchengine.services;

import searchengine.dto.index.SiteDto;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;

public interface SiteService {
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
    Site findSite(Integer id, String name, String url);
    Site findSiteById(Integer id);
    Site saveSite(Site site);
    void deleteSite(Site site);
    List<Site> findAllSites();
    public List<Page> findPagesBySite(SiteDto siteDto);
    Page savePage(Page page);
    void deletePage(Page page);
    void deletePageByUrl(String url);
    void deleteLemmaByPage(Page page);
    int getPagesCount(SiteDto siteDto);

    /**
     * Возвращает суммарное количество лемм на сайте
     * @param lemma объект Lemma, содержащая информацию о лемме,
     *             для поиска по определенному сайту нужно установить значение site,
     *             если null то считается кол-во всех лемм в БД
     * @return int суммарное количество лемм на сайте или в БД
     */
    Integer сountLemmasOnSite(Lemma lemma);
}
