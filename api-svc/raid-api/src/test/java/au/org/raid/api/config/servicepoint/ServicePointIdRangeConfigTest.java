package au.org.raid.api.config.servicepoint;

import au.org.raid.api.config.properties.IdentifierProperties;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static au.org.raid.api.config.servicepoint.ServicePointIdRangeConfig.SERVICE_POINT_ID_START_PLACEHOLDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ServicePointIdRangeConfigTest {
    private static final String ARDC_ROR = "https://ror.org/038sjwq14";

    private IdentifierProperties identifierProperties;
    private ServicePointIdRangeConfig subject;

    @BeforeEach
    void setUp() {
        identifierProperties = new IdentifierProperties();
        subject = new ServicePointIdRangeConfig(new RegistrationAgencyRegister(), identifierProperties);
    }

    @Test
    @DisplayName("sets the Flyway placeholder from the configured agency's allocation")
    void setsPlaceholder() {
        identifierProperties.setRegistrationAgencyIdentifier(ARDC_ROR);
        final var configuration = new FluentConfiguration();

        subject.servicePointIdRangeCustomizer().customize(configuration);

        assertThat(configuration.getPlaceholders())
                .containsEntry(SERVICE_POINT_ID_START_PLACEHOLDER, "20000000");
    }

    @Test
    @DisplayName("keeps placeholders that were already configured")
    void preservesExistingPlaceholders() {
        identifierProperties.setRegistrationAgencyIdentifier(ARDC_ROR);
        final var configuration = new FluentConfiguration()
                .placeholders(java.util.Map.of("existing", "value"));

        subject.servicePointIdRangeCustomizer().customize(configuration);

        assertThat(configuration.getPlaceholders())
                .containsEntry("existing", "value")
                .containsEntry(SERVICE_POINT_ID_START_PLACEHOLDER, "20000000");
    }

    @Test
    @DisplayName("fails before Flyway runs when the agency is not registered")
    void failsForUnregisteredAgency() {
        identifierProperties.setRegistrationAgencyIdentifier("https://ror.org/notallocated");
        final var configuration = new FluentConfiguration();

        assertThatThrownBy(() -> subject.servicePointIdRangeCustomizer().customize(configuration))
                .isInstanceOf(RegistrationAgencyNotRegisteredException.class);

        assertThat(configuration.getPlaceholders())
                .doesNotContainKey(SERVICE_POINT_ID_START_PLACEHOLDER);
    }

    @Test
    @DisplayName("fails startup even when Flyway is disabled and the customizer never runs")
    void failsWhenFlywayDisabled() {
        identifierProperties.setRegistrationAgencyIdentifier(null);

        assertThatThrownBy(() -> subject.afterPropertiesSet())
                .isInstanceOf(RegistrationAgencyNotRegisteredException.class)
                .hasMessageContaining("raid.identifier.registration-agency-identifier is not set");
    }

    @Test
    @DisplayName("starts normally for a registered agency")
    void startsForRegisteredAgency() {
        identifierProperties.setRegistrationAgencyIdentifier(ARDC_ROR);

        assertThatNoException().isThrownBy(() -> subject.afterPropertiesSet());
    }
}
