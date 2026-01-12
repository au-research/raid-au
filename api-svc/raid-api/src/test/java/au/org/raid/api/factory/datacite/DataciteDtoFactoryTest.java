package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteAttributesDto;
import au.org.raid.api.model.datacite.doi.DataciteIdentifier;
import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataciteDtoFactoryTest {
    @Mock
    private DataciteAttributesDtoFactory attributesDtoFactory;
    @Mock
    private DataciteIdentifierFactory identifierFactory;
    @InjectMocks
    private DataciteDtoFactory dtoFactory;

    @Test
    @DisplayName("create() sets all fields on mint")
    void createOnMint() {
        final var request = new RaidCreateRequest();
        final var handle = "_handle";

        final var attributes = new DataciteAttributesDto();
        when(attributesDtoFactory.create(request, handle)).thenReturn(attributes);

        final var identifier = new DataciteIdentifier();
        when(identifierFactory.create(eq(handle), eq("DOI"))).thenReturn(identifier);

        final var result = dtoFactory.create(request, handle);

        assertThat(result.getSchemaVersion(), is("http://datacite.org/schema/kernel-4"));
        assertThat(result.getType(), is("dois"));
        assertThat(result.getDataciteIdentifiers(), is(List.of(identifier)));
        assertThat(result.getAttributes(), is(attributes));
    }

    @Test
    @DisplayName("create() sets all fields on update")
    void createOnUpdate() {
        final var request = new RaidUpdateRequest();
        final var handle = "_handle";

        final var attributes = new DataciteAttributesDto();
        when(attributesDtoFactory.create(request, handle)).thenReturn(attributes);

        final var identifier = new DataciteIdentifier();
        when(identifierFactory.create(eq(handle), eq("DOI"))).thenReturn(identifier);

        final var result = dtoFactory.create(request, handle);

        assertThat(result.getSchemaVersion(), is("http://datacite.org/schema/kernel-4"));
        assertThat(result.getType(), is("dois"));
        assertThat(result.getDataciteIdentifiers(), is(List.of(identifier)));
        assertThat(result.getAttributes(), is(attributes));
    }

}