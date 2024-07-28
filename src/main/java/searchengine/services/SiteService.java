package searchengine.services;

import searchengine.dto.index.SiteDto;
import searchengine.model.IndexingStatus;
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
    public void updateLastErrorOnSite(Site site, String error);
    public void updateStatusOnSite(Site site, IndexingStatus newIndexingStatus);
    Site saveSite(Site site);
    void deleteAllSite();
    void deleteSite(Site site);
    List<Site> findAllSites();
    public List<Page> findPagesBySite(SiteDto siteDto);
    Page savePage(Page page);
    void deletePage(Page page);
    void deletePageByUrl(String url);

    /**
     * Удаляет леммы из счетчиков и БД (таблицы index_t, lemma)
     * содержащиеся на указанной странице. Это действие требуется
     * перед удалением страницы.
     * @param page
     */
    void deleteLemmaByPage(Page page);

    /**
     * Количество страниц в БД или на конкретном сайте.
     * @param siteDto siteDto объект site, для которого будет произведена выборка
     * @return int количество страниц
     */
    int countPagesOnSite(SiteDto siteDto);

    /**
     * Возвращает суммарное количество лемм на сайте
     *
     * @param siteDto экземпляр сущности siteDto или null
     * @return int суммарное количество лемм на сайте или в БД
     */
    int сountAllLemmasOnSite(SiteDto siteDto);
    public long getStatusTime(SiteDto siteDto);
}
