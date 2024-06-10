package searchengine;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication

public class Application {
    private static boolean stopIndexing = false;
    private static boolean isIndexingRunning = false;
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

public static void setStopIndexing(boolean stop) {
        stopIndexing = stop;
}

public static boolean isStopIndexing()  {
        return stopIndexing;
}

public static void setIndexingRunning(boolean running)  {
        isIndexingRunning = running;
}

public static boolean isIndexingRunning()  {
        return isIndexingRunning;
}

}
