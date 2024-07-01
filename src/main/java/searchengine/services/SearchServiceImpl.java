package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.response.SearchResponse;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final MorphologyService morphologyService;

    @Override
    public SearchResponse search(String query, int offset, int limit) {
        log.info("Searching for {}", query);
        return new SearchResponse();
    }

    private List<String> getWords(String query) {
        //String[] words = morphologyService.getWords(query);

        return null;

    }
}
