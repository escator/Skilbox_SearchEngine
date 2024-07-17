package searchengine.services;

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

}
