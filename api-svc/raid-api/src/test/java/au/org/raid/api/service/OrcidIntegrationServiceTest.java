package au.org.raid.api.service;

import au.org.raid.api.config.properties.OrcidIntegrationProperties;
import au.org.raid.api.dto.ContributorLookupResponse;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.db.jooq.tables.records.ContributorRecord;
import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcidIntegrationServiceTest {

    @Mock
    private OrcidIntegrationClient orcidIntegrationClient;

    @Mock
    private ContributorRepository contributorRepository;

    private OrcidIntegrationService orcidIntegrationService;

    // Use direct executor for testing to avoid async complexity
    private final Executor directExecutor = Runnable::run;

    @BeforeEach
    void setUp() {
        orcidIntegrationService = createService(true, "http://localhost:1080");
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private OrcidIntegrationService createService(final boolean enabled, final String host) {
        return createService(enabled, host, "api-key");
    }

    private OrcidIntegrationService createService(final boolean enabled, final String host, final String apiKey) {
        final var properties = new OrcidIntegrationProperties();
        properties.setEnabled(enabled);
        properties.setHost(host);
        properties.setApiKey(apiKey);

        return new OrcidIntegrationService(
                orcidIntegrationClient,
                directExecutor,
                contributorRepository,
                properties
        );
    }

    private static RaidDto raidOwnedBy(final long servicePointId) {
        return new RaidDto()
                .identifier(new Id()
                        .id("https://raid.org/10.82841/86bf1b6c")
                        .owner(new Owner().servicePoint(BigDecimal.valueOf(servicePointId))))
                .contributor(List.of(new Contributor().id("orcid1")));
    }

    @Test
    void constructor_shouldDefaultToDisabled_whenPropertiesNotSet() {
        final var service = new OrcidIntegrationService(
                orcidIntegrationClient, directExecutor, contributorRepository, new OrcidIntegrationProperties());

        service.notifyRaidChanged(raidOwnedBy(99L));

        verifyNoInteractions(orcidIntegrationClient);
    }

    @Test
    void constructor_shouldRefuseToStart_whenEnabledWithoutHost() {
        final var exception = assertThrows(IllegalStateException.class, () -> createService(true, " "));

        assertThat(exception.getMessage(), containsString("raid.orcid-integration.host"));
    }

    @Test
    void constructor_shouldRefuseToStart_whenEnabledWithoutApiKey() {
        final var exception = assertThrows(IllegalStateException.class,
                () -> createService(true, "http://localhost:1080", " "));

        assertThat(exception.getMessage(), containsString("raid.orcid-integration.api-key"));
    }

    @Test
    void constructor_shouldStart_whenDisabledWithoutHost() {
        createService(false, "");
    }

    @Test
    void constructor_shouldStart_whenDisabledWithoutApiKey() {
        createService(false, "", null);
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
    void notifyRaidChanged_shouldNotCallUpdater_whenDisabled() {
        final var service = createService(false, "");

        service.notifyRaidChanged(raidOwnedBy(99L));

        verifyNoInteractions(orcidIntegrationClient);
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
    void notifyRaidChanged_shouldSendHandle_whenNoTransactionIsActive() {
        orcidIntegrationService.notifyRaidChanged(raidOwnedBy(99L));

        verify(orcidIntegrationClient).notifyRaidChanged("10.82841/86bf1b6c");
    }

    @Test
    void notifyRaidChanged_shouldWaitForCommit_whenTransactionIsActive() {
        TransactionSynchronizationManager.initSynchronization();

        orcidIntegrationService.notifyRaidChanged(raidOwnedBy(99L));

        verifyNoInteractions(orcidIntegrationClient);

        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());

        verify(orcidIntegrationClient).notifyRaidChanged("10.82841/86bf1b6c");
    }

    @Test
    void notifyRaidChanged_shouldNotNotify_whenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();

        orcidIntegrationService.notifyRaidChanged(raidOwnedBy(99L));

        // A rollback completes the synchronization without calling afterCommit.
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(orcidIntegrationClient);
    }

    @Test
    void notifyRaidChanged_shouldNotThrow_whenUpdaterFails() {
        doThrow(new RuntimeException("Connection refused"))
                .when(orcidIntegrationClient).notifyRaidChanged("10.82841/86bf1b6c");

        assertDoesNotThrow(() -> orcidIntegrationService.notifyRaidChanged(raidOwnedBy(99L)));
    }
}
