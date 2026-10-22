package au.org.raid.api.config.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * RAID-993: binds the shipped YAML so a change to the defaults is caught here rather than on a deployment.
 */
class OrcidIntegrationPropertiesTest {

    private static OrcidIntegrationProperties bind(final String... resources) throws IOException {
        final var environment = new StandardEnvironment();
        final var loader = new YamlPropertySourceLoader();

        // Later resources take precedence, as a profile-specific file does over application.yaml
        for (final var resource : resources) {
            loader.load(resource, new ClassPathResource(resource))
                    .forEach(source -> environment.getPropertySources().addFirst(source));
        }

        return Binder.get(environment)
                .bind("raid.orcid-integration", OrcidIntegrationProperties.class)
                .get();
    }

    @Test
    void defaultConfiguration_disablesIntegration() throws IOException {
        final var properties = bind("application.yaml");

        assertThat(properties.isEnabled(), is(false));
        assertThat(properties.getHost(), is(emptyOrNullString()));
    }

    @Test
    void defaultConfiguration_doesNotPointAtLocalMock() throws IOException {
        final var properties = bind("application.yaml");

        assertThat(properties.getRaidListener().getUri(), not(containsString("localhost")));
        assertThat(properties.getContributorIdLookup().getUri(), not(containsString("localhost")));
    }

    @Test
    void devProfile_enablesIntegrationAgainstLocalMock() throws IOException {
        final var properties = bind("application.yaml", "application-dev.yaml");

        assertThat(properties.isEnabled(), is(true));
        assertThat(properties.getHost(), equalTo("http://localhost:1080"));
        assertThat(properties.getRaidListener().getUri(), equalTo("http://localhost:1080/raid-update"));
        assertThat(properties.getContributorIdLookup().getUri(),
                equalTo("http://localhost:1080/find-contributor-by-orcid"));
    }

    @Test
    void unsetFields_defaultToDisabled() {
        assertThat(new OrcidIntegrationProperties().isEnabled(), is(false));
    }
}
