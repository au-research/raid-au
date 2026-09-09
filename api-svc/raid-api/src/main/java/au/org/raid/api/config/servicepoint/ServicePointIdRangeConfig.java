package au.org.raid.api.config.servicepoint;

import au.org.raid.api.config.properties.IdentifierProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * Resolves this instance's Service Point ID range from its Registration Agency
 * ROR and exposes it to Flyway as the {@code servicePointIdStart} placeholder.
 *
 * <p>Runs while the Flyway bean is being built, so an instance whose agency has
 * no allocated range fails before any migration executes. That ordering matters:
 * the database is left untouched and the previously deployed version keeps
 * serving, rather than a half-applied renumbering being committed.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ServicePointIdRangeConfig implements InitializingBean {
    public static final String SERVICE_POINT_ID_START_PLACEHOLDER = "servicePointIdStart";
    public static final String SERVICE_POINT_ID_BLOCK_SIZE_PLACEHOLDER = "servicePointIdBlockSize";

    private final RegistrationAgencyRegister register;
    private final IdentifierProperties identifierProperties;

    /**
     * Fails startup for a deployment that never builds a Flyway bean, such as one
     * with {@code spring.flyway.enabled: false}, where the customizer below would
     * otherwise never run and an unallocated instance could mint regardless.
     */
    @Override
    public void afterPropertiesSet() {
        register.servicePointIdStart(identifierProperties.getRegistrationAgencyIdentifier());
    }

    @Bean
    public FlywayConfigurationCustomizer servicePointIdRangeCustomizer() {
        return configuration -> {
            final var ror = identifierProperties.getRegistrationAgencyIdentifier();
            final var start = register.servicePointIdStart(ror);

            log.info("Registration Agency {} is allocated Service Point IDs from {}", ror, start);

            final var placeholders = new HashMap<>(configuration.getPlaceholders());
            placeholders.put(SERVICE_POINT_ID_START_PLACEHOLDER, Long.toString(start));
            placeholders.put(SERVICE_POINT_ID_BLOCK_SIZE_PLACEHOLDER, Long.toString(register.blockSize()));
            configuration.placeholders(placeholders);
        };
    }
}
