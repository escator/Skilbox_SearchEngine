package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.dto.index.HtmlParseResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Сервис для парсинга html страниц
 */
@Slf4j

public class HtmlParseService {
    private final String url;
    private final String root;
    boolean isReady = false;
    Document document;
    private String agent = "Mozilla/5.0  (Windows; U; WindowsNT5.1; en-US;  rv:1.8.1.13) Gecko/20080311 Firefox/2.0.0.13";
    private String referrer  = "http://www.google.com";

    public HtmlParseService(String url, String root) {
        this.url = url;
        this.root = root;
    }

    public HtmlParseService(String url, String root, String agent, String referrer) {
        this.url = url;
        this.root = root;
        this.agent  = agent;
        this.referrer   = referrer;
    }

    /**
     * Парсит данных из html страницы по указанному url
     * @return Document - данные из html страницы
     */
    public HtmlParseResponse parse() {
        if  (isReady)  {
            return new HtmlParseResponse(document, HttpStatus.OK);
        }
        try {
            document = Jsoup.connect(url)
                    .userAgent(agent)
                    .referrer(referrer)
                    .get();
            isReady= true;
        } catch (IOException e) {
            log.error("Error while parsing html from " + url, e);
            document = null;
        }
        return new HtmlParseResponse(document, HttpStatus.OK);
    }

    /**
     * Возвращает список всех ссылок на странице с указанным url
     * @return LinkedHashSet<String> - список всех ссылок
     */
    public List<String> getAllLinksOnPage() {
        if (!isReady) {
            document = parse().getDocument();
        }
        if (document == null) {
            return new ArrayList<>();
        }
        Elements links = document.select("a[href]");

        LinkedHashSet<String> linkSet = new LinkedHashSet<>();
        for (Element element : links) {
            linkSet.add(element.attr("href"));
        }
        return new ArrayList<>(normalizeLinks(linkSet));
    }


    /** Фильтрует и приводит список ссылок к нормализованному виду
     *
     * @param links
     * @return
     */
    private LinkedHashSet<String> normalizeLinks(Set<String> links) {
        LinkedHashSet<String> normalizedLinks = new LinkedHashSet<>();
        for (String link : links) {
            if (!link.contains("#")){
                if (link.endsWith("/") && link.length() > 1) {
                    link = link.substring(0, link.length() - 1);
                }
                if (link.startsWith("/") && link.length() > 1) {
                    normalizedLinks.add(root + link.substring(1));
                } else if (link.startsWith(url) && link.length() > url.length()) {
                    normalizedLinks.add(link);
                }
            }
        }
        return normalizedLinks;
    }

}