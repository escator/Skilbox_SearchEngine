package searchengine.util;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Chernyakov Pavel
 * Класс содержит служебные утилиты предназначенные для обработки Url-адресов.
 */
public class LinkToolsBox {

    /**
     * Приводит список Url-адресов к абсолютному виду.
     * @param links
     * @param rootUrl
     * @return
     */
    public static LinkedHashSet<String> normalizeLinks(Set<String> links, String rootUrl) {
        LinkedHashSet<String> normalizedLinks = new LinkedHashSet<>();
        for (String link : links) {
            String res;
            if ((res = normalizeUrl(link, rootUrl)) != null) {
                normalizedLinks.add(res);
            }
        }
        return normalizedLinks;
    }

    /**
     * Отфильтровывает якоря (#) и внешние ссылки, приводит ссылки и
     * относительные и абсолютные к единому абсолютному виду.
     * example: http://site.com/page
     * @param url отрабатываемая строка url ссылкию.
     * @param rootUrl корневой адрес    ex: http://site.com
     * @return String абсолютнаяя ссылка или null если аргумент не являетс url.
     */
    public static String normalizeUrl(String url, String rootUrl) {
        String result = null;
        url = url.strip();
        if (isUrl(url)) {
            if (url.endsWith("/") && url.length() > 1) {
                url = url.substring(0, url.length() - 1);
            }
            if (url.startsWith("/") && url.length() > 1) {
                result = rootUrl + url;
            } else if (url.startsWith("/") && url.length() == 1) {
                result = rootUrl;
            } else if (url.startsWith(rootUrl) && url.length() > rootUrl.length()) {
                result = url;
            } else if (url.startsWith(rootUrl)  && url.length() == rootUrl.length())  {
                result  = rootUrl;
            }
            if (!isInternalUrl(result, rootUrl))  {
                return null;
            }
        }
        return result;
    }

    /**
     * Проверяет является ли адрес url, удаляет завершающий слеш если он есть
     * @param url String Адрес url.
     * @return String Адрес url без завершающего слеша.
     */
    public static String normalizeRootUrl(String url)  {
        String res = null;
        url  = url.strip();
        if (isUrl(url)) {
            if (url.endsWith("/")) {
                res = url.substring(0, url.length() - 1);
            } else {
                res  = url;
            }
        }
        return res;
    }

    /**
     * Проверяет является ли строка url
     * @param url String
     * @return boolean true - Адрес url.
     */
    public static boolean isUrl(String url) {
        boolean result = true;
        url  = url.strip();
        if (!(url.startsWith("http://") ||
                url.startsWith("https://") ||
                url.startsWith("/"))) {
            result = false;
        }
        if (url.contains("#")) {
            result = false;
        }
        return result;
    }

    /**
     * Проперяет является ли адрес url внутренней ссылкой
     * @param url String Адрес url.
     * @param rootUrl корневой адрес    ex: http://site.com
     * @return boolean true  - Адрес url внутренней ссылкой.
     *     false  - Адрес url не внутренней ссылкой или null.
     */
    public static boolean isInternalUrl(String url, String rootUrl)  {
        boolean result = true;
        if (url == null || !url.startsWith(rootUrl))  {
            result = false;
        }
        return result;
    }

    /**
     * Возвращает относительную ссылку из абсолютной
     * @param url String Адрес url.
     * @param rootUrl корневой адрес    ex: http://site.com
     * @return String относительная ссылка. Если url null, возвращает null.
     */
    public static String getShortUrl(String url, String rootUrl) {
        if  (url  == null) return null;
        url   = url.strip();
        if (url.endsWith("/")) {
            url.substring(0, url.length() - 1);
        }
        if (url == rootUrl) {
            return "/";
        }
        return url.substring(rootUrl.length());
    }
    public static String getShortUrl(String url) {
        String rootUrl= extractRootDomain(url);
        return getShortUrl(url, rootUrl);
    }

    /**
     * Извлекает из переданного url адреса корневой домен
     * @param url String Адрес url. (абсолютная ссылка
     * @return String корневой домен. Если url null, возвращает null.
     */
    public static String extractRootDomain(String url) {
        if (url  == null) return null;
        int start = url.indexOf("://",  0);
        int end = 0;
        if (start > 0) {
            end = url.indexOf('/', start + 3);
        } else {
            end  = url.indexOf('/',  0);
        }
        if (end < 0) end = url.length();
        return url.substring(0, end);
    }

    public static boolean isValidUrl(String url) {
      return true;
    }
}
