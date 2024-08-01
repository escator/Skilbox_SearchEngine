package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "jsop-cfg")
public class JsopConnectionCfg {
    private String agent;
    private String referrer;
    Set<Integer> validCodes;
}
