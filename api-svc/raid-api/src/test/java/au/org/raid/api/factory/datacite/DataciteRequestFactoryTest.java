package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteDto;
import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataciteRequestFactoryTest {
    @Mock
    private DataciteDtoFactory dataciteDtoFactory;
    @InjectMocks
    private DataciteRequestFactory dataciteRequestFactory;

    @Test
    @DisplayName("create() returns create request")
    void returnsDataciteRequestForCreate() {
        final var handle = "_handle";
        final var request = new RaidCreateRequest();

        final var dataciteDto = new DataciteDto();

        when(dataciteDtoFactory.create(request, handle)).thenReturn(dataciteDto);

        final var result = dataciteRequestFactory.create(request, handle);

        assertThat(result.getData(), is(dataciteDto));
    }

    @Test
    @DisplayName("create() returns update request")
    void returnsDataciteRequestForUpdate() {
        final var handle = "_handle";
        final var request = new RaidUpdateRequest();

        final var dataciteDto = new DataciteDto();

        when(dataciteDtoFactory.create(request, handle)).thenReturn(dataciteDto);

        final var result = dataciteRequestFactory.create(request, handle);

        assertThat(result.getData(), is(dataciteDto));
    }
}