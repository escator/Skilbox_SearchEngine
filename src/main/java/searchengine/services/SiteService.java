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

    /**
     * Найти в БД сайт, по DTO контейнеру SiteDTO
     * @param siteDto SiteDto объект с параметрами поиска
     * @return Site сущность или null
     */
    public Site findSiteByDTO(SiteDto siteDto);

    public void updateLastErrorOnSite(Site site, String error);
    public void updateStatusOnSite(Site site, IndexingStatus newIndexingStatus);
    Site saveSite(Site site);
    void deleteAllSite();
    void deleteSite(Site site);

    /**
     * Получить список всех страниц сайта
     * @param siteDto siteDto объект задающий параметры поиска
     * @return List<Page> список найденных страниц
     *         если DTO == null, то вернет пустой список
     */
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
     *                или null для подсчета по всей БД
     * @return int количество страниц
     */
    int countPagesOnSite(SiteDto siteDto);

    /**
     * Подсчитывает количество лемм на сайте или в во всей БД
     *
     * @param siteDto экземпляр сущности siteDto или null
     *                для подсчета по всей БД
     * @return int суммарное количество лемм на сайте или в БД
     */
    int сountAllLemmasOnSite(SiteDto siteDto);

    /**
     * Получить время последнего обновления статуса сайта в формате timestamp
     * @param siteDto siteDto экземпляр сущности siteDto или null
     * @return long timestamp последнего обновления статуса сайта в формате timestamp,
     *              если DTO == null, то возвращает 0
     */
    public long getStatusTime(SiteDto siteDto);
}
