package searchengine.response;

import searchengine.dto.search.SearchItemData;

import java.util.List;

public class SearchResponse {
    boolean result;
    int count;
    List<SearchItemData> data;
}
