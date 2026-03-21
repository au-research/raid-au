package au.org.raid.api.service;

import au.org.raid.api.dto.ContributorLookupResponse;
import au.org.raid.api.dto.RaidListenerMessage;
import au.org.raid.api.factory.RaidListenerMessageFactory;
import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcidIntegrationServiceTest {

    @Mock
    private OrcidIntegrationClient orcidIntegrationClient;

    @Mock
    private RaidListenerMessageFactory messageFactory;

    @Mock
    private ServicePointService servicePointService;

    private OrcidIntegrationService orcidIntegrationService;

    // Use direct executor for testing to avoid async complexity
    private final Executor directExecutor = Runnable::run;

    @BeforeEach
    void setUp() {
        orcidIntegrationService = new OrcidIntegrationService(
                orcidIntegrationClient,
                directExecutor,
                messageFactory,
                servicePointService
        );
    }

    @Test
    void setContributorStatus_shouldReturnContributorsWithUpdatedStatus_whenOrcidLookupSucceeds() {
        // Given
        final var contributor1 = new Contributor().id("orcid1");
        final var contributor2 = new Contributor().id("orcid2");
        final var contributors = List.of(contributor1, contributor2);

        final var lookupResponse1 = ContributorLookupResponse.builder().status("AUTHENTICATED").build();
        final var lookupResponse2 = ContributorLookupResponse.builder().status("VERIFIED").build();

        when(orcidIntegrationClient.findByOrcid("orcid1"))
                .thenReturn(Optional.of(lookupResponse1));
        when(orcidIntegrationClient.findByOrcid("orcid2"))
                .thenReturn(Optional.of(lookupResponse2));

        // When
        final var result = orcidIntegrationService.setContributorStatus(contributors);

        // Then
        assertThat(result, hasSize(2));
        assertThat(result.get(0).getStatus(), equalTo("AUTHENTICATED"));
        assertThat(result.get(1).getStatus(), equalTo("VERIFIED"));

        verify(orcidIntegrationClient).findByOrcid("orcid1");
        verify(orcidIntegrationClient).findByOrcid("orcid2");
    }

    @Test
    void setContributorStatus_shouldSetAwaitingAuthentication_whenOrcidLookupReturnsEmpty() {
        // Given
        final var contributor = new Contributor().id("orcid1");
        final var contributors = List.of(contributor);

        when(orcidIntegrationClient.findByOrcid("orcid1"))
                .thenReturn(Optional.empty());

        // When
        final var result = orcidIntegrationService.setContributorStatus(contributors);

        // Then
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getStatus(), equalTo("AWAITING_AUTHENTICATION"));

        verify(orcidIntegrationClient).findByOrcid("orcid1");
    }

    @Test
    void setContributorStatus_shouldSetAwaitingAuthentication_whenOrcidLookupThrowsException() {
        // Given
        final var contributor = new Contributor().id("orcid1");
        final var contributors = List.of(contributor);

        when(orcidIntegrationClient.findByOrcid("orcid1"))
                .thenThrow(new RuntimeException("Network error"));

        // When
        final var result = orcidIntegrationService.setContributorStatus(contributors);

        // Then
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getStatus(), equalTo("AWAITING_AUTHENTICATION"));

        verify(orcidIntegrationClient).findByOrcid("orcid1");
    }

    @Test
    void setContributorStatus_shouldHandleEmptyList() {
        // Given
        final var contributors = List.<Contributor>of();

        // When
        final var result = orcidIntegrationService.setContributorStatus(contributors);

        // Then
        assertThat(result, empty());
        verifyNoInteractions(orcidIntegrationClient);
    }

    @Test
    void updateOrcidRecord_shouldPostMessageForEachContributor() {
        // Given
        final var contributor1 = new Contributor().id("orcid1");
        final var contributor2 = new Contributor().id("orcid2");
        final var servicePointId = 99L;
        final var id = new Id()
                .id("raid123")
                .owner(new Owner().servicePoint(servicePointId));
        final var titles = List.of(new Title().text("Test Raid"));

        final var raid = new RaidDto()
                .identifier(id)
                .title(titles)
                .contributor(List.of(contributor1, contributor2));

        final var servicePoint = new ServicePoint()
                .id(servicePointId);

        when(servicePointService.findById(servicePointId)).thenReturn(Optional.of(servicePoint));

        final var message1 = new RaidListenerMessage();

        when(messageFactory.create(raid, servicePoint))
                .thenReturn(message1);

        // When
        orcidIntegrationService.updateOrcidRecord(raid);

        // Then
        verify(messageFactory).create(raid, servicePoint);
        verify(orcidIntegrationClient).post(message1);
    }
}