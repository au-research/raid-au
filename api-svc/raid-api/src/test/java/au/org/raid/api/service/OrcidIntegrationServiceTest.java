package au.org.raid.api.service;

import au.org.raid.api.config.properties.OrcidIntegrationProperties;
import au.org.raid.api.dto.ContributorLookupResponse;
import au.org.raid.api.dto.RaidListenerMessage;
import au.org.raid.api.factory.RaidListenerMessageFactory;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.db.jooq.tables.records.ContributorRecord;
import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Mock
    private ContributorRepository contributorRepository;

    private OrcidIntegrationService orcidIntegrationService;

    // Use direct executor for testing to avoid async complexity
    private final Executor directExecutor = Runnable::run;

    @BeforeEach
    void setUp() {
        orcidIntegrationService = createService(true, "http://localhost:1080");
    }

    private OrcidIntegrationService createService(final boolean enabled, final String host) {
        final var properties = new OrcidIntegrationProperties();
        properties.setEnabled(enabled);
        properties.setHost(host);

        return new OrcidIntegrationService(
                orcidIntegrationClient,
                directExecutor,
                messageFactory,
                servicePointService,
                contributorRepository,
                properties
        );
    }

    private static RaidDto raidOwnedBy(final long servicePointId) {
        return new RaidDto()
                .identifier(new Id()
                        .id("raid123")
                        .owner(new Owner().servicePoint(BigDecimal.valueOf(servicePointId))))
                .contributor(List.of(new Contributor().id("orcid1")));
    }

    @Test
    void constructor_shouldDefaultToDisabled_whenPropertiesNotSet() {
        final var service = new OrcidIntegrationService(
                orcidIntegrationClient, directExecutor, messageFactory, servicePointService,
                contributorRepository, new OrcidIntegrationProperties());

        service.updateOrcidRecord(raidOwnedBy(99L));

        verifyNoInteractions(orcidIntegrationClient);
    }

    @Test
    void constructor_shouldRefuseToStart_whenEnabledWithoutHost() {
        final var exception = assertThrows(IllegalStateException.class, () -> createService(true, " "));

        assertThat(exception.getMessage(), containsString("raid.orcid-integration.host"));
    }

    @Test
    void constructor_shouldStart_whenDisabledWithoutHost() {
        createService(false, "");
    }

    @Test
    void setContributorStatus_shouldNotCallIntegration_whenDisabled() {
        final var service = createService(false, "");
        final var contributors = List.of(new Contributor().id("orcid1"));

        when(contributorRepository.findByPid("orcid1")).thenReturn(Optional.empty());

        service.setContributorStatus(contributors);

        verifyNoInteractions(orcidIntegrationClient);
    }

    @Test
    void setContributorStatus_shouldSetUnauthenticated_whenDisabledAndContributorIsNew() {
        final var service = createService(false, "");
        final var contributors = List.of(new Contributor().id("orcid1").status("AUTHENTICATED"));

        when(contributorRepository.findByPid("orcid1")).thenReturn(Optional.empty());

        final var result = service.setContributorStatus(contributors);

        assertThat(result.get(0).getStatus(), equalTo("UNAUTHENTICATED"));
    }

    @Test
    void setContributorStatus_shouldKeepStoredStatus_whenDisabledAndContributorExists() {
        final var service = createService(false, "");
        final var contributors = List.of(new Contributor().id("orcid1"));

        when(contributorRepository.findByPid("orcid1"))
                .thenReturn(Optional.of(new ContributorRecord().setPid("orcid1").setStatus("AUTHENTICATED")));

        final var result = service.setContributorStatus(contributors);

        assertThat(result.get(0).getStatus(), equalTo("AUTHENTICATED"));
    }

    @Test
    void setContributorStatus_shouldSetUnauthenticated_whenDisabledAndStoredStatusIsBlank() {
        final var service = createService(false, "");
        final var contributors = List.of(new Contributor().id("orcid1"));

        when(contributorRepository.findByPid("orcid1"))
                .thenReturn(Optional.of(new ContributorRecord().setPid("orcid1")));

        final var result = service.setContributorStatus(contributors);

        assertThat(result.get(0).getStatus(), equalTo("UNAUTHENTICATED"));
    }

    @Test
    void setContributorStatus_shouldNotReadStoredStatus_whenEnabled() {
        when(orcidIntegrationClient.findByOrcid("orcid1")).thenReturn(Optional.empty());

        orcidIntegrationService.setContributorStatus(List.of(new Contributor().id("orcid1")));

        verifyNoInteractions(contributorRepository);
    }

    @Test
    void updateOrcidRecord_shouldNotCallIntegration_whenDisabled() {
        final var service = createService(false, "");

        service.updateOrcidRecord(raidOwnedBy(99L));

        verifyNoInteractions(orcidIntegrationClient, messageFactory, servicePointService);
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
                .owner(new Owner().servicePoint(BigDecimal.valueOf(servicePointId)));
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