package searchengine.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import searchengine.AppContextProvider;
import searchengine.config.JsopConnectionCfg;
import searchengine.response.HtmlParseResponse;
import searchengine.util.LinkToolsBox;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис для парсинга html страниц.
 * сервис работает вне контекста Spring.
 */
@Slf4j

public class HtmlParseService {
    private final String url;
    private final String root;
    private final String agent;
    private final String referrer;
    boolean isReady = false;
    private Document document;
    @Getter
    private HtmlParseResponse response;



    public HtmlParseService(String url, String root) {
        this.url = url;
        this.root = root;
        JsopConnectionCfg jsopConnectionCfg = (JsopConnectionCfg) AppContextProvider.getBean("jsopConnectionCfg");
        this.agent  = jsopConnectionCfg.getAgent();
        this.referrer   = jsopConnectionCfg.getReferrer();
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

        Set<String> linkSet = links.stream().map(e -> e.attr("href")).collect(Collectors.toSet());
        return LinkToolsBox.normalizeLinks(linkSet, root);
    }

}
