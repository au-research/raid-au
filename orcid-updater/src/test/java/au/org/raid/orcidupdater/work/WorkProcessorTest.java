package au.org.raid.orcidupdater.work;

import au.org.raid.orcidupdater.orcid.OrcidRaid;
import au.org.raid.orcidupdater.orcid.OrcidWriter;
import au.org.raid.orcidupdater.raidapi.RaidApiUnavailableException;
import au.org.raid.orcidupdater.raidapi.RaidServiceClient;
import au.org.raid.orcidupdater.serviceauth.ServiceTokenException;
import au.org.raid.orcidupdater.status.ContributorStatus;
import au.org.raid.orcidupdater.status.ContributorStatusService;
import au.org.raid.orcidupdater.support.TestProperties;
import au.org.raid.orcidupdater.work.WorkRepository.Kind;
import au.org.raid.orcidupdater.work.WorkRepository.WorkItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("WorkProcessor")
@ExtendWith(OutputCaptureExtension.class)
class WorkProcessorTest {
    private static final String BARE_ID = "0000-0002-1825-0097";
    private static final Instant NOW = Instant.parse("2026-10-27T01:00:00Z");
    private static final String ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String HANDLE = "10.82841/86bf1b6c";
    private static final String EMBARGOED = "https://vocabularies.coar-repositories.org/access_rights/c_f1cf/";

    private final WorkRepository workRepository = mock(WorkRepository.class);
    private final WorkQueue workQueue = mock(WorkQueue.class);
    private final RaidServiceClient raidServiceClient = mock(RaidServiceClient.class);
    private final ContributorStatusService statusService = mock(ContributorStatusService.class);
    private final OrcidWriter orcidWriter = mock(OrcidWriter.class);
    private final WorkProcessor processor = new WorkProcessor(workRepository, workQueue, raidServiceClient,
            statusService, orcidWriter, TestProperties.complete(), Clock.fixed(NOW, ZoneOffset.UTC));

    private static WorkItem status(final int attempts) {
        return new WorkItem(Kind.STATUS, ID, 3, attempts);
    }

    private static WorkItem sync() {
        return new WorkItem(Kind.RAID_SYNC, HANDLE, 1, 0);
    }

    private static JsonNode raid(final String accessType) throws Exception {
        return new ObjectMapper().readTree("""
                {"identifier": {"id": "https://raid.org/10.82841/86bf1b6c", "owner": {"id": "https://ror.org/038sjwq14"}},
                 "access": {"type": {"id": "%s"}},
                 "contributor": [{"id": "%s", "position": []}]}
                """.formatted(accessType, ID));
    }

    @BeforeEach
    void setUp() {
        when(raidServiceClient.updateContributorStatus(any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("Each poll queues expired invitations, then claims a batch until the claim timeout")
    void poll() {
        when(workRepository.claimDue(NOW, NOW.plus(Duration.ofMinutes(5)), 20)).thenReturn(List.of(sync()));
        when(raidServiceClient.findRaid(HANDLE)).thenReturn(Optional.empty());

        assertThat(processor.processDue()).isEqualTo(1);

        verify(workRepository).requestStatusForExpiredInvitations(NOW);
        verify(workRepository).complete(sync());
    }

    @Nested
    @DisplayName("Status")
    class Status {
        @Test
        @DisplayName("Pushes the worked-out status and finishes")
        void pushes() {
            when(statusService.statusOf(ID)).thenReturn(ContributorStatus.AWAITING_AUTHENTICATION);

            processor.process(status(0));

            verify(raidServiceClient).updateContributorStatus(ID, ContributorStatus.AWAITING_AUTHENTICATION);
            verify(raidServiceClient, never()).findHandlesByContributor(any());
            verify(workRepository).complete(status(0));
        }

        @Test
        @DisplayName("AUTHENTICATED: every RAiD naming the iD is queued for ORCID")
        void authenticatedQueuesRaids() {
            when(statusService.statusOf(ID)).thenReturn(ContributorStatus.AUTHENTICATED);
            when(raidServiceClient.findHandlesByContributor(ID)).thenReturn(List.of(HANDLE, "10.82841/2"));

            processor.process(status(0));

            verify(workQueue).requestRaidSync(HANDLE);
            verify(workQueue).requestRaidSync("10.82841/2");
            verify(workRepository).complete(status(0));
        }

        @Test
        @DisplayName("The RAiD API has no contributor with this iD: finished, nothing to retry")
        void noContributor() {
            when(statusService.statusOf(ID)).thenReturn(ContributorStatus.UNAUTHENTICATED);
            when(raidServiceClient.updateContributorStatus(any(), any())).thenReturn(false);

            processor.process(status(0));

            verify(workRepository).complete(status(0));
        }
    }

    @Nested
    @DisplayName("RAiD sync")
    class Sync {
        @Test
        @DisplayName("Open RAiD: written to ORCID")
        void open() throws Exception {
            when(raidServiceClient.findRaid(HANDLE)).thenReturn(Optional.of(raid(WorkProcessor.OPEN_ACCESS)));

            processor.process(sync());

            final var written = ArgumentCaptor.forClass(OrcidRaid.class);
            verify(orcidWriter).sync(written.capture());
            assertThat(written.getValue().handle()).isEqualTo(HANDLE);
            assertThat(written.getValue().contributors()).extracting(OrcidRaid.Contributor::id).containsExactly(ID);
            verify(workRepository).complete(sync());
        }

        @Test
        @DisplayName("Embargoed RAiD: held back, nothing written, finished")
        void embargoed() throws Exception {
            when(raidServiceClient.findRaid(HANDLE)).thenReturn(Optional.of(raid(EMBARGOED)));

            processor.process(sync());

            verifyNoInteractions(orcidWriter);
            verify(workRepository).complete(sync());
        }

        @Test
        @DisplayName("RAiD with no access type: held back, as it can't be shown to be open")
        void noAccess() throws Exception {
            when(raidServiceClient.findRaid(HANDLE)).thenReturn(Optional.of(new ObjectMapper().readTree("{}")));

            processor.process(sync());

            verifyNoInteractions(orcidWriter);
        }

        @Test
        @DisplayName("RAiD not found: nothing written, finished")
        void notFound() {
            when(raidServiceClient.findRaid(HANDLE)).thenReturn(Optional.empty());

            processor.process(sync());

            verifyNoInteractions(orcidWriter);
            verify(workRepository).complete(sync());
        }
    }

    @Nested
    @DisplayName("Failures")
    class Failures {
        @Test
        @DisplayName("RAiD API unreachable: tried again after the initial backoff, doubled per attempt")
        void backoff() {
            when(statusService.statusOf(ID)).thenReturn(ContributorStatus.AUTHENTICATED);
            when(raidServiceClient.updateContributorStatus(any(), any()))
                    .thenThrow(new RaidApiUnavailableException("RAiD API unreachable"));

            processor.process(status(0));
            processor.process(status(2));

            verify(workRepository).recordFailure(status(0), "RAiD API unreachable", NOW, NOW.plusSeconds(30));
            verify(workRepository).recordFailure(status(2), "RAiD API unreachable", NOW, NOW.plusSeconds(120));
            verify(workRepository, never()).complete(any());
        }

        @Test
        @DisplayName("Keycloak refuses the updater's token: tried again later")
        void tokenRefused() {
            when(raidServiceClient.findRaid(HANDLE)).thenThrow(new ServiceTokenException("Keycloak returned 401 (invalid_client)"));

            processor.process(sync());

            verify(workRepository).recordFailure(sync(), "Keycloak returned 401 (invalid_client)", NOW, NOW.plusSeconds(30));
        }

        @Test
        @DisplayName("Last attempt fails: recorded as failed for operators, not tried again")
        void givesUp() {
            when(statusService.statusOf(ID)).thenReturn(ContributorStatus.AUTHENTICATED);
            when(raidServiceClient.updateContributorStatus(any(), any()))
                    .thenThrow(new RaidApiUnavailableException("RAiD API returned 503"));

            processor.process(status(4));

            verify(workRepository).recordFailure(eq(status(4)), eq("RAiD API returned 503"), eq(NOW), isNull());
        }

        @Test
        @DisplayName("Unexpected exception: its message isn't stored, only its type")
        void unexpected() {
            when(statusService.statusOf(ID)).thenThrow(new IllegalStateException("something with data in it"));

            processor.process(status(0));

            final var failure = ArgumentCaptor.forClass(String.class);
            verify(workRepository).recordFailure(eq(status(0)), failure.capture(), eq(NOW), any());
            assertThat(failure.getValue()).contains("IllegalStateException").doesNotContain("data in it");
        }

        @Test
        @DisplayName("A huge attempt count doesn't overflow the backoff")
        void noOverflow() {
            final var properties = TestProperties.complete();
            properties.getWork().setMaxAttempts(1000);
            final var patient = new WorkProcessor(workRepository, workQueue, raidServiceClient, statusService,
                    orcidWriter, properties, Clock.fixed(NOW, ZoneOffset.UTC));
            when(raidServiceClient.findRaid(HANDLE)).thenThrow(new RaidApiUnavailableException("down"));

            patient.process(new WorkItem(Kind.RAID_SYNC, HANDLE, 1, 900));

            final var next = ArgumentCaptor.forClass(Instant.class);
            verify(workRepository).recordFailure(any(), any(), eq(NOW), next.capture());
            assertThat(next.getValue()).isEqualTo(NOW.plusSeconds(30L << 16));
        }
    }

    @Nested
    @DisplayName("Logging")
    class Logging {
        @Test
        @DisplayName("A status pushed for an authenticated contributor is logged without their ORCID iD")
        void statusPushed(final CapturedOutput output) {
            when(statusService.statusOf(ID)).thenReturn(ContributorStatus.AUTHENTICATED);
            when(raidServiceClient.findHandlesByContributor(ID)).thenReturn(List.of(HANDLE));

            processor.process(status(0));

            assertThat(output.getAll()).contains("Status AUTHENTICATED set for a contributor")
                    .contains("1 RAiD(s) queued for ORCID").doesNotContain(BARE_ID);
        }

        @Test
        @DisplayName("A status the RAiD API has no contributor for is logged without the ORCID iD")
        void noContributor(final CapturedOutput output) {
            when(statusService.statusOf(ID)).thenReturn(ContributorStatus.UNAUTHENTICATED);
            when(raidServiceClient.updateContributorStatus(any(), any())).thenReturn(false);

            processor.process(status(0));

            assertThat(output.getAll()).contains("no contributor with this iD").doesNotContain(BARE_ID);
        }

        @Test
        @DisplayName("Retrying, giving up and unexpected failures on a STATUS item name the kind, not the iD")
        void statusFailures(final CapturedOutput output) {
            when(statusService.statusOf(ID)).thenReturn(ContributorStatus.AUTHENTICATED);
            when(raidServiceClient.updateContributorStatus(any(), any()))
                    .thenThrow(new RaidApiUnavailableException("RAiD API returned 504 setting a contributor's status"));

            processor.process(status(0));
            processor.process(status(4));
            when(statusService.statusOf(ID)).thenThrow(new IllegalStateException("boom"));
            processor.process(status(0));

            assertThat(output.getAll()).contains("STATUS for a contributor: attempt 1 failed")
                    .contains("STATUS for a contributor: gave up after 5 attempts")
                    .contains("STATUS for a contributor: unexpected failure")
                    .doesNotContain(BARE_ID);
        }

        @Test
        @DisplayName("A RAID_SYNC item's failures name the RAiD handle")
        void syncFailures(final CapturedOutput output) {
            when(raidServiceClient.findRaid(HANDLE)).thenThrow(new RaidApiUnavailableException("down"));

            processor.process(sync());

            assertThat(output.getAll()).contains("RAID_SYNC " + HANDLE + ": attempt 1 failed");
        }

        @Test
        @DisplayName("A synced RAiD logs how many contributors had each outcome, not who they were")
        void syncOutcomeCounts(final CapturedOutput output) throws Exception {
            when(raidServiceClient.findRaid(HANDLE)).thenReturn(Optional.of(raid(WorkProcessor.OPEN_ACCESS)));
            when(orcidWriter.sync(any())).thenReturn(List.of(
                    new OrcidWriter.EntryOutcome(ID, OrcidWriter.Outcome.NOT_AUTHENTICATED),
                    new OrcidWriter.EntryOutcome("https://orcid.org/0000-0001-5109-3700", OrcidWriter.Outcome.NOT_AUTHENTICATED)));

            processor.process(sync());

            assertThat(output.getAll()).contains("RAiD " + HANDLE + " synced to ORCID: {NOT_AUTHENTICATED=2}")
                    .doesNotContain(BARE_ID).doesNotContain("0000-0001-5109-3700");
        }
    }

    @Test
    @DisplayName("Nothing due: no claims processed")
    void nothingDue() {
        when(workRepository.claimDue(any(), any(), anyInt())).thenReturn(List.of());

        assertThat(processor.processDue()).isZero();
        verifyNoInteractions(raidServiceClient, orcidWriter);
    }
}
