package searchengine.dto.search;

import lombok.Data;

/**
 * Данные найденного документа.
 */
@Data
public class SearchItemData {
    String site;
    String siteName;
    String uri;
    String title;
    String snippet;
    Double relevance;
}
