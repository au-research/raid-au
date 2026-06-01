package au.org.raid.api.config.database;

import org.jooq.conf.Settings;
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqConfig {

    @Bean
    public DefaultConfigurationCustomizer jooqConfigurationCustomizer() {
        return configuration -> {
            Settings settings = configuration.settings();
            settings.withRenderSchema(false);
        };
    }
}
