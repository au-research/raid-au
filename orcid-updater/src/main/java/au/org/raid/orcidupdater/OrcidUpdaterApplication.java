package au.org.raid.orcidupdater;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OrcidUpdaterApplication {
    public static void main(final String[] args) {
        SpringApplication.run(OrcidUpdaterApplication.class, args);
    }
}
