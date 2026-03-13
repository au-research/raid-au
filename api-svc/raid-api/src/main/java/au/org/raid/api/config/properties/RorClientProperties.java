package au.org.raid.api.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "raid.ror-client")
public class RorClientProperties {
    private String clientId;
    private String baseUrl;
}
