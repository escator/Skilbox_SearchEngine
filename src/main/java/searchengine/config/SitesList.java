package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import searchengine.util.LinkToolsBox;

import java.util.List;


@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesList {
    private List<SiteDto> sites;

    public List<SiteDto> getSites() {
        return sites.stream().map(
                (site) -> {
                    site.setUrl(LinkToolsBox.normalizeRootUrl(site.getUrl()));
                    return site;}
                ).collect(java.util.stream.Collectors.toList());
    }
}
