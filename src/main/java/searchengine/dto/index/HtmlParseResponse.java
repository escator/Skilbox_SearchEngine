package searchengine.dto.index;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;

@Setter
@Getter
@AllArgsConstructor
public class HtmlParseResponse {
    private Document document;
    private HttpStatus status;
}
