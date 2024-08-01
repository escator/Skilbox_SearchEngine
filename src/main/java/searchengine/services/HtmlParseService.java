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
import java.net.HttpURLConnection;
import java.net.URL;
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
    @Getter
    private HtmlParseResponse response;
    private final Set<Integer> validResponseCode;

    public HtmlParseService(String url, String root) {
        this.url = url;
        this.root = root;
        JsopConnectionCfg jsopConnectionCfg = (JsopConnectionCfg) AppContextProvider.getBean("jsopConnectionCfg");
        this.agent  = jsopConnectionCfg.getAgent();
        this.referrer   = jsopConnectionCfg.getReferrer();
        this.validResponseCode = jsopConnectionCfg.getValidCodes();
    }

    /**
     * Парсит данные из html страницы по указанному url
     * @return HtmlResponse - содержащий код ответа и Document
     */
    public HtmlParseResponse parse() {
        // Установленный флаг isReady - флаг, показывающий, что данные уже были
        // загружены. Позволяет избежать повторной загрузки данных при повторном
        // обращении к данному экземпляру
        if  (isReady)  {
            return response;
        }

        response  = new HtmlParseResponse();
        int responseCode = checkConnectionOnUrl(url);
        if (validResponseCode.contains(responseCode)) {
            try {
                log.info("[Getting BODY url {}] Status code: {}", url, responseCode);
                Document document = Jsoup.connect(url)
                        .userAgent(agent)
                        .referrer(referrer)
                        .get();
                isReady= true;
                response.setDocument(document);
            } catch (IOException e) {
                log.info("Oшибка при получении данных из {}" + url);
                e.printStackTrace();
            }

        } else {
            response.setDocument(new Document(""));
        }
        response.setStatus(responseCode);
        return response;
    }

    private int checkConnectionOnUrl(String url) {
        int responseCode = 0;
        try {
            HttpURLConnection huc = (HttpURLConnection) new URL(url).openConnection();
            responseCode = huc.getResponseCode();
            log.debug("Content-Type: {} url {}", huc.getContentType(), url);
            if (validResponseCode.contains(responseCode)
                    && !huc.getHeaderField("Content-Type").contains("text/html")) {
                responseCode = 415;
            }
            huc.disconnect();
        } catch (IOException e) {
            log.info("IOException: при подключении к url: " + url);
            e.printStackTrace();
        }
        return responseCode;
    }

    /**
     * Возвращает список всех ссылок на странице с указанным url
     * @return LinkedHashSet<String> - список всех ссылок
     */
    public Set<String> getAllLinksOnPage() {
        if (!isReady) {
            parse().getDocument();
        }
        if (response.getDocument() == null) {
            return new HashSet<>();
        }
        Elements links = response.getDocument().select("a[href]");

        Set<String> linkSet = links.stream().map(e -> e.attr("href")).collect(Collectors.toSet());
        return LinkToolsBox.normalizeLinks(linkSet, root);
    }

}
