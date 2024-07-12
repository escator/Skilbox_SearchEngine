package searchengine.exception;

public class NullArgException extends Exception {
    public NullArgException(String message) {
        super(message);
    }

    public NullArgException(String message, Throwable cause) {
        super(message, cause);
    }
}
