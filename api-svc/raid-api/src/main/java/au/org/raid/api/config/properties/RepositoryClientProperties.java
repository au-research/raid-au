package au.org.raid.api.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "raid.repository-client")
public class RepositoryClientProperties {
    private String username;
    private String password;
    private String url;
    private String email;
}
