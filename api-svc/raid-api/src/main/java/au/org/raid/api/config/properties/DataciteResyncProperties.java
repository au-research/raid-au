package au.org.raid.api.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "raid.datacite.resync")
@Data
public class DataciteResyncProperties {
    private boolean enabled;
    private int batchSize = 50;
    private long throttleMillis = 1000;
    private long pollDelayMillis = 60000;
}
