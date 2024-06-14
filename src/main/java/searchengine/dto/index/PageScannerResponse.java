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

    public static PageScannerResponse getStopResponse() {
        PageScannerResponse resp = new PageScannerResponse();
        resp.setStatus(resp.status.STOPPED);
        resp.setMessage("Индексирование остановлено пользователем");
        return resp;
    }
    public static PageScannerResponse getOKResponse() {
        PageScannerResponse resp = new PageScannerResponse();
        resp.setStatus(resp.status.OK);
        return resp;
    }

    public static PageScannerResponse getErrorResponse() {
        PageScannerResponse resp = new PageScannerResponse();
        resp.setStatus(resp.status.ERROR);
        resp.setMessage("Ошибка при получении html данных страницы");
        return resp;
    }
}
