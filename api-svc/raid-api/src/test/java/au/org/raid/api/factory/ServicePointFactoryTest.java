package au.org.raid.api.factory;

import au.org.raid.db.jooq.tables.records.ServicePointRecord;
import au.org.raid.idl.raidv2.model.ServicePoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class ServicePointFactoryTest {
    final ServicePointFactory factory = new ServicePointFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var id = 123L;
        final var name = "_name";
        final var adminEmail = "admin-email";
        final var techEmail = "tech-email";
        final var enabled = true;
        final var appWritesEnabled = true;

        final var servicePoint = new ServicePoint()
                .id(id)
                .name(name)
                .adminEmail(adminEmail)
                .techEmail(techEmail)
                .enabled(enabled)
                .appWritesEnabled(appWritesEnabled);

        final var result = factory.create(servicePoint);

        assertThat(result.getId(), is(id));
        assertThat(result.getName(), is(name));
        assertThat(result.getAdminEmail(), is(adminEmail));
        assertThat(result.getTechEmail(), is(techEmail));
        assertThat(result.getEnabled(), is(enabled));
        assertThat(result.getAppWritesEnabled(), is(appWritesEnabled));
    }

    @Test
    @DisplayName("createPublic() copies only id, name and identifierOwner")
    void createPublicCopiesOnlyPublicFields() {
        final var record = new ServicePointRecord();
        record.setId(123L);
        record.setName("_name");
        record.setIdentifierOwner("https://ror.org/_owner");
        record.setAdminEmail("admin-email");
        record.setTechEmail("tech-email");
        record.setRepositoryId("repo");
        record.setPrefix("10.1234");
        record.setGroupId("group");
        record.setEnabled(true);
        record.setAppWritesEnabled(true);

        final var result = factory.createPublic(record);

        assertThat(result.getId(), is(123L));
        assertThat(result.getName(), is("_name"));
        assertThat(result.getIdentifierOwner(), is("https://ror.org/_owner"));
        // the model type has no other properties, so nothing sensitive can be serialised
        assertThat(Arrays.stream(result.getClass().getDeclaredFields())
                .map(Field::getName)
                .filter(n -> !n.equals("serialVersionUID") && !n.equals("id") && !n.equals("name") && !n.equals("identifierOwner"))
                .toList(), is(List.of()));
    }
}
