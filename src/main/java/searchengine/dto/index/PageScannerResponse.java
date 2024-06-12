package searchengine.dto.index;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageScannerResponse {
    public enum status {OK, STOPPED, DOUBLE_LINK, ERROR};

    public PageScannerResponse(PageScannerResponse.status status) {
        this.status = status;
    }

    private status status;
    private String message;
}
