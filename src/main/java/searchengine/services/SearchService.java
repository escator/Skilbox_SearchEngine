package searchengine.services;

import searchengine.response.SearchResponse;

public interface SearchService {
    public SearchResponse search(String query, int offset, int limit, String siteUrl);
}
