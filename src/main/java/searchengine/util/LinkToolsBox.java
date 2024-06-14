package searchengine.util;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Chernyakov Pavel
 * Класс содержит служебные утилиты предназначенные для обработки Url-адресов.
 */
public class LinkToolsBox {

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
     * Отфильтровывает якоря (#) и приводит ссылки и относительные и
     * абсолютные к единому абсолютному виду.
     * example: http://site.com/page
     * @param url отрабатываемая строка url ссылкию.
     * @param rootUrl корневой адрес    ex: http://site.com
     * @return String абсолютнаяя ссылка или null если аргумент не являетс url.
     */
    public static String normalizeUrl(String url, String rootUrl) {
        String result = null;
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

    public static String normalizeRootUrl(String url)  {
        String res = null;
        if (isUrl(url)) {
            if (url.endsWith("/")) {
                res = url.substring(0, url.length() - 1);
            } else {
                res  = url;
            }
        }
        return res;
    }

    public static boolean isUrl(String url) {
        boolean result = true;
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

    public static boolean isInternalUrl(String url, String rootUrl)  {
        boolean result = true;
        if (url == null || !url.startsWith(rootUrl))  {
            result = false;
        }
        return result;
    }

    public static String getShortUrl(String url, String rootUrl) {
        if (url.endsWith("/")) {
            url.substring(0, url.length() - 1);
        }
        if (url == rootUrl) {
            return "/";
        }
        return url.substring(rootUrl.length());
    }


}
