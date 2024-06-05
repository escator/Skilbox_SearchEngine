package searchengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class IndexingResponse {
    public IndexingResponse(boolean result) {
        this.result = result;
        this.error = null;
    }

    private final boolean result;
    private final String error;
}
