package searchengine.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import searchengine.dto.index.HtmlParseResponse;
import searchengine.util.LinkToolsBox;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    @Getter
    HtmlParseResponse response;
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
     * Парсит данные из html страницы по указанному url
     * @return HtmlResponse - содержащий код ответа и Document
     */
    public HtmlParseResponse parse() {
        if  (isReady)  {
            return new HtmlParseResponse(document, HttpStatus.OK.value());
        }

        response  = new HtmlParseResponse();
        Connection connection  = Jsoup.connect(url)
                .userAgent(agent)
                .referrer(referrer);

        try {
            Connection.Response responseJsoup = connection.execute();
            log.info("Status code:" + responseJsoup.statusCode() + " [Getting BODY from " + url + "]");
            document = responseJsoup.parse();
            isReady= true;
            response.setDocument(document);
            response.setStatus(responseJsoup.statusCode());
        } catch (IOException e) {
            response.setDocument(new Document(""));
            response.setStatus(400);
        }
        return response;
    }

    /**
     * Возвращает список всех ссылок на странице с указанным url
     * @return LinkedHashSet<String> - список всех ссылок
     */
    public Set<String> getAllLinksOnPage() {
        if (!isReady) {
            document = parse().getDocument();
        }
        if (document == null) {
            return new HashSet<>();
        }
        Elements links = document.select("a[href]");

        LinkedHashSet<String> linkSet = new LinkedHashSet<>();
        for (Element element : links) {
            linkSet.add(element.attr("href"));
        }
        return LinkToolsBox.normalizeLinks(linkSet, root);
    }

}
