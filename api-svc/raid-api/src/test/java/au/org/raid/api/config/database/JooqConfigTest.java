package au.org.raid.api.config.database;

import org.jooq.impl.DefaultConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JooqConfigTest {

    private final JooqConfig jooqConfig = new JooqConfig();

    @Test
    @DisplayName("JOOQ customizer disables schema rendering")
    void disablesSchemaRendering() throws Exception {
        var configuration = new DefaultConfiguration();

        jooqConfig.jooqConfigurationCustomizer().customize(configuration);

        assertThat(configuration.settings().isRenderSchema()).isFalse();
    }
}
