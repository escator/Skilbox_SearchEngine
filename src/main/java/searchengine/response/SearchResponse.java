package searchengine.response;

import lombok.Data;
import searchengine.dto.search.SearchItemData;

import java.util.List;
@Data
public class SearchResponse {
    boolean result;
    int count;
    List<SearchItemData> data;
}
