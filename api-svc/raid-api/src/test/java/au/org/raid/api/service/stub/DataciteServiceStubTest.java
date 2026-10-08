package au.org.raid.api.service.stub;

import au.org.raid.api.factory.datacite.DataciteRequestFactory;
import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// The stub is constructed with no RestTemplate, so any attempt to call DataCite would NPE.
@ExtendWith(MockitoExtension.class)
class DataciteServiceStubTest {
    private static final String DOI_HANDLE = "10.5072/abc123";
    private static final String NON_DOI_HANDLE = "102.100.100/abc123";
    private static final String REPOSITORY_ID = "ATHH.TEST";
    private static final String PASSWORD = "password";

    @Mock
    private DataciteRequestFactory dataciteRequestFactory;

    private DataciteServiceStub stub;

    @BeforeEach
    void setUp() {
        stub = new DataciteServiceStub(dataciteRequestFactory, new ObjectMapper(), 0L);
    }

    @Test
    @DisplayName("mint should build the DataCite request without sending it")
    void mintBuildsRequestWithoutSending() {
        final var request = new RaidCreateRequest();

        assertDoesNotThrow(() -> stub.mint(request, DOI_HANDLE, REPOSITORY_ID, PASSWORD));

        verify(dataciteRequestFactory).create(request, DOI_HANDLE);
    }

    @Test
    @DisplayName("update with a RaidUpdateRequest should build the DataCite request without sending it")
    void updateRequestBuildsRequestWithoutSending() {
        final var request = new RaidUpdateRequest();

        assertDoesNotThrow(() -> stub.update(request, DOI_HANDLE, REPOSITORY_ID, PASSWORD));

        verify(dataciteRequestFactory).create(request, DOI_HANDLE);
    }

    @Test
    @DisplayName("update with a RaidDto should build the DataCite request without sending it")
    void updateDtoBuildsRequestWithoutSending() {
        final var request = new RaidDto();

        assertDoesNotThrow(() -> stub.update(request, DOI_HANDLE, REPOSITORY_ID, PASSWORD));

        verify(dataciteRequestFactory).create(request, DOI_HANDLE);
    }

    @Test
    @DisplayName("mint should skip non-DOI handles, as the real service does")
    void mintSkipsNonDoiHandle() {
        stub.mint(new RaidCreateRequest(), NON_DOI_HANDLE, REPOSITORY_ID, PASSWORD);

        verifyNoInteractions(dataciteRequestFactory);
    }

    @Test
    @DisplayName("mint should propagate request-building failures, as the real service does")
    void mintPropagatesRequestFactoryFailure() {
        when(dataciteRequestFactory.create(any(RaidCreateRequest.class), anyString()))
                .thenThrow(new IllegalStateException("ROR lookup failed"));

        assertThrows(IllegalStateException.class,
                () -> stub.mint(new RaidCreateRequest(), DOI_HANDLE, REPOSITORY_ID, PASSWORD));
    }
}
