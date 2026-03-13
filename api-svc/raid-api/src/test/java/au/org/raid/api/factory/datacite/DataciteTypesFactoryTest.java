package au.org.raid.api.factory.datacite;

import au.org.raid.api.vocabularies.datacite.ResourceTypeGeneral;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class DataciteTypesFactoryTest {
    private static final String RAID_RESOURCE_TYPE = "RAiD";
    private final DataciteTypesFactory typesFactory = new DataciteTypesFactory();

    @Test
    @DisplayName("Creates types with resourceTypeGeneral of 'Other'")
    void setsResourceTypeGeneral() {
        final var result = typesFactory.create();

        assertThat(result.getResourceTypeGeneral(), is(ResourceTypeGeneral.PROJECT.getName()));
        assertThat(result.getResourceType(), is(RAID_RESOURCE_TYPE));
    }
}