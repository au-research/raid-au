package au.org.raid.api.factory.record;

import au.org.raid.idl.raidv2.model.ServicePointCreateRequest;
import au.org.raid.idl.raidv2.model.ServicePointUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ServicePointRecordFactoryTest {
    private static final String NAME = "_name";
    private static final String IDENTIFIER_OWNER = "identifier-owner";
    private static final String ADMIN_EMAIL = "admin-email";
    private static final String TECH_EMAIL = "tech-email";
    private static final boolean APP_WRITES_ENABLED = true;
    private static final boolean ENABLED = true;
    private final ServicePointRecordFactory factory = new ServicePointRecordFactory();

    @Test
    @DisplayName("Sets all fields from ServicePointUpdateRequest")
    void setsAllFieldsFromServicePoint() {
        final var id = 123L;

        final var servicePoint = new ServicePointUpdateRequest()
                .id(id)
                .name(NAME)
                .identifierOwner(IDENTIFIER_OWNER)
                .adminEmail(ADMIN_EMAIL)
                .techEmail(TECH_EMAIL)
                .appWritesEnabled(APP_WRITES_ENABLED)
                .enabled(ENABLED);

        final var result = factory.create(servicePoint);

        assertThat(result.getId(), is(id));
        assertThat(result.getName(), is(NAME));
        assertThat(result.getIdentifierOwner(), is(IDENTIFIER_OWNER));
        assertThat(result.getAdminEmail(), is(ADMIN_EMAIL));
        assertThat(result.getTechEmail(), is(TECH_EMAIL));
        assertThat(result.getAppWritesEnabled(), is(APP_WRITES_ENABLED));
        assertThat(result.getEnabled(), is(ENABLED));
    }


    @Test
    @DisplayName("Sets all fields from ServicePointCreateRequest")
    void setsAllFieldsFromCreateServicePointRequest() {
        final var repositoryId = "TEST.REPO123";
        final var prefix = "10.12345";
        final var password = "password123";

        final var servicePoint = new ServicePointCreateRequest()
                .name(NAME)
                .identifierOwner(IDENTIFIER_OWNER)
                .adminEmail(ADMIN_EMAIL)
                .techEmail(TECH_EMAIL)
                .appWritesEnabled(APP_WRITES_ENABLED)
                .enabled(ENABLED);

        final var result = factory.create(servicePoint, repositoryId, prefix, password);

        assertThat(result.getName(), is(NAME));
        assertThat(result.getIdentifierOwner(), is(IDENTIFIER_OWNER));
        assertThat(result.getAdminEmail(), is(ADMIN_EMAIL));
        assertThat(result.getTechEmail(), is(TECH_EMAIL));
        assertThat(result.getAppWritesEnabled(), is(APP_WRITES_ENABLED));
        assertThat(result.getEnabled(), is(ENABLED));
        assertThat(result.getRepositoryId(), is(repositoryId));
        assertThat(result.getPrefix(), is(prefix));
        assertThat(result.getPassword(), is(password));
    }

    @Test
    @DisplayName("Should trim whitespace from input")
    void trimInput() {
        final var repositoryId = " TEST.REPO123 ";
        final var prefix = " 10.12345 ";
        final var password = "password123";

        final var servicePoint = new ServicePointCreateRequest()
                .name(NAME)
                .identifierOwner(IDENTIFIER_OWNER)
                .adminEmail(" " + ADMIN_EMAIL + " ")
                .techEmail(" " + TECH_EMAIL + " ")
                .appWritesEnabled(APP_WRITES_ENABLED)
                .enabled(ENABLED);

        final var result = factory.create(servicePoint, repositoryId, prefix, password);

        assertThat(result.getName(), is(NAME));
        assertThat(result.getIdentifierOwner(), is(IDENTIFIER_OWNER));
        assertThat(result.getAdminEmail(), is(ADMIN_EMAIL));
        assertThat(result.getTechEmail(), is(TECH_EMAIL));
        assertThat(result.getAppWritesEnabled(), is(APP_WRITES_ENABLED));
        assertThat(result.getEnabled(), is(ENABLED));
        assertThat(result.getRepositoryId(), is("TEST.REPO123"));
        assertThat(result.getPrefix(), is("10.12345"));
    }
}