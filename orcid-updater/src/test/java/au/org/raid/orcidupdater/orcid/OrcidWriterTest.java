package au.org.raid.orcidupdater.orcid;

import au.org.raid.orcidupdater.crypto.TokenCipher;
import au.org.raid.orcidupdater.invitation.InvitationRepository;
import au.org.raid.orcidupdater.orcid.OrcidEntryRepository.OrcidEntry;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Contributor;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Position;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Title;
import au.org.raid.orcidupdater.orcid.OrcidWriteException.Kind;
import au.org.raid.orcidupdater.orcid.OrcidWriter.EntryOutcome;
import au.org.raid.orcidupdater.orcid.OrcidWriter.Outcome;
import au.org.raid.orcidupdater.researcher.ResearcherRepository;
import au.org.raid.orcidupdater.support.TestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RAID-1004 scenarios, with ORCID, ROR and the database stubbed. */
@DisplayName("OrcidWriter")
class OrcidWriterTest {
    private static final Instant NOW = Instant.parse("2026-10-27T01:00:00Z");
    private static final String HANDLE = "10.82841/86bf1b6c";
    private static final String ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String OTHER_ID = "https://orcid.org/0000-0001-5109-3700";
    private static final String OWNER = "https://ror.org/038sjwq14";
    private static final String PRINCIPAL_INVESTIGATOR = "https://vocabulary.raid.org/contributor.position.schema/307";
    private static final String CO_INVESTIGATOR = "https://vocabulary.raid.org/contributor.position.schema/308";
    private static final OrcidOrganisation ORGANISATION = new OrcidOrganisation("Australian Research Data Commons",
            "Melbourne", "Victoria", "AU", OWNER);

    private final OrcidMemberClient orcid = mock(OrcidMemberClient.class);
    private final RorClient rorClient = mock(RorClient.class);
    private final OrcidEntryRepository entryRepository = mock(OrcidEntryRepository.class);
    private final ResearcherRepository researcherRepository = mock(ResearcherRepository.class);
    private final InvitationRepository invitationRepository = mock(InvitationRepository.class);
    private final TokenCipher tokenCipher = new TokenCipher(TestProperties.complete());
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private OrcidWriter writer;

    private static OrcidRaid raid(final Contributor... contributors) {
        return new OrcidRaid(HANDLE, "https://raid.org/" + HANDLE, OWNER,
                List.of(new Title("Coral reef resilience", MembershipFactory.PRIMARY_TITLE_TYPE, "2025-03-01", null)),
                List.of(contributors));
    }

    private static Contributor contributor(final String id, final String position) {
        return new Contributor(id, List.of(new Position(position, "2025-03-01", null)));
    }

    private void authenticated(final String id) {
        when(researcherRepository.findAccessToken(id)).thenReturn(Optional.of(tokenCipher.encrypt("token-" + id, id)));
    }

    @BeforeEach
    void setUp() {
        final var properties = TestProperties.complete();
        properties.getOrcidWrite().setInitialBackoff(Duration.ZERO);
        writer = new OrcidWriter(orcid, new MembershipFactory(clock), rorClient, entryRepository, researcherRepository,
                invitationRepository, tokenCipher, new TransactionTemplate(mock(PlatformTransactionManager.class)),
                properties, clock);
        when(entryRepository.find(anyString(), anyString())).thenReturn(Optional.empty());
        when(entryRepository.findByRaidHandle(HANDLE)).thenReturn(List.of());
        when(researcherRepository.findAccessToken(anyString())).thenReturn(Optional.empty());
        when(rorClient.find(OWNER)).thenReturn(Optional.of(ORGANISATION));
    }

    @Nested
    @DisplayName("Writing")
    class Writing {
        @Test
        @DisplayName("Create an entry: POST with the decrypted token, put code stored, under the iD's lock")
        void create() {
            authenticated(ID);
            when(orcid.create(eq(ID), eq("token-" + ID), any())).thenReturn(1234567L);

            assertThat(writer.sync(raid(contributor(ID, CO_INVESTIGATOR))))
                    .containsExactly(new EntryOutcome(ID, Outcome.WRITTEN));

            verify(invitationRepository).lockOrcidId(ID);
            verify(entryRepository).recordSuccess(ID, HANDLE, 1234567L, NOW);
        }

        @Test
        @DisplayName("Update an existing entry: PUT by put code, with the put code in the body")
        @SuppressWarnings("unchecked")
        void update() {
            authenticated(ID);
            when(entryRepository.find(ID, HANDLE)).thenReturn(Optional.of(new OrcidEntry(ID, HANDLE, 1234567L)));

            assertThat(writer.sync(raid(contributor(ID, CO_INVESTIGATOR)))).extracting(EntryOutcome::outcome)
                    .containsExactly(Outcome.WRITTEN);

            final var body = ArgumentCaptor.forClass(Map.class);
            verify(orcid).update(eq(ID), eq("token-" + ID), eq(1234567L), body.capture());
            assertThat(body.getValue()).containsEntry("put-code", 1234567L);
            verify(orcid, never()).create(any(), any(), any());
            verify(entryRepository).recordSuccess(ID, HANDLE, 1234567L, NOW);
        }

        @Test
        @DisplayName("Stored put code no longer exists at ORCID: the entry is created again and the new put code kept")
        void recreate() {
            authenticated(ID);
            when(entryRepository.find(ID, HANDLE)).thenReturn(Optional.of(new OrcidEntry(ID, HANDLE, 1234567L)));
            doThrow(new OrcidWriteException(Kind.NOT_FOUND, "ORCID returned 404"))
                    .when(orcid).update(any(), any(), anyLong(), any());
            when(orcid.create(eq(ID), any(), any())).thenReturn(7654321L);

            writer.sync(raid(contributor(ID, CO_INVESTIGATOR)));

            verify(entryRepository).recordSuccess(ID, HANDLE, 7654321L, NOW);
        }

        @Test
        @DisplayName("Every entry names the RAiD owner's organisation, looked up in ROR once per RAiD")
        @SuppressWarnings("unchecked")
        void organisation() {
            authenticated(ID);
            authenticated(OTHER_ID);
            final var body = ArgumentCaptor.forClass(Map.class);
            when(orcid.create(any(), any(), body.capture())).thenReturn(1L, 2L);

            writer.sync(raid(contributor(ID, PRINCIPAL_INVESTIGATOR), contributor(OTHER_ID, CO_INVESTIGATOR)));

            assertThat(body.getAllValues()).allSatisfy(membership -> assertThat(((Map<String, Object>) membership)
                    .get("organization")).asString().contains(OWNER));
            verify(rorClient, times(1)).find(OWNER);
        }

        @Test
        @DisplayName("Owner organisation not found in ROR: failure recorded, no ORCID call, since ORCID requires one")
        void organisationUnknown() {
            authenticated(ID);
            when(rorClient.find(OWNER)).thenReturn(Optional.empty());

            assertThat(writer.sync(raid(contributor(ID, CO_INVESTIGATOR))))
                    .containsExactly(new EntryOutcome(ID, Outcome.FAILED));

            verifyNoInteractions(orcid);
            verify(entryRepository).recordFailure(eq(ID), eq(HANDLE), contains("couldn't be found in ROR"), eq(NOW));
        }

        @Test
        @DisplayName("iD not authenticated (or revoked earlier): no ORCID call, nothing recorded")
        void notAuthenticated() {
            assertThat(writer.sync(raid(contributor(ID, CO_INVESTIGATOR))))
                    .containsExactly(new EntryOutcome(ID, Outcome.NOT_AUTHENTICATED));

            verifyNoInteractions(orcid);
            verify(entryRepository, never()).recordSuccess(any(), any(), anyLong(), any());
            verify(entryRepository, never()).recordFailure(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Each contributor is written independently: one failure doesn't stop the others")
        void independent() {
            authenticated(ID);
            authenticated(OTHER_ID);
            when(orcid.create(eq(ID), any(), any())).thenThrow(new OrcidWriteException(Kind.REJECTED, "ORCID returned 400"));
            when(orcid.create(eq(OTHER_ID), any(), any())).thenReturn(2L);

            assertThat(writer.sync(raid(contributor(ID, CO_INVESTIGATOR), contributor(OTHER_ID, CO_INVESTIGATOR))))
                    .containsExactly(new EntryOutcome(ID, Outcome.FAILED), new EntryOutcome(OTHER_ID, Outcome.WRITTEN));
        }
    }

    @Nested
    @DisplayName("Failures")
    class Failures {
        @Test
        @DisplayName("Token revoked by the researcher: marked revoked, failure recorded, no retry")
        void tokenRevoked() {
            authenticated(ID);
            when(orcid.create(any(), any(), any())).thenThrow(new OrcidWriteException(Kind.TOKEN_REJECTED, "ORCID returned 401"));

            assertThat(writer.sync(raid(contributor(ID, CO_INVESTIGATOR))))
                    .containsExactly(new EntryOutcome(ID, Outcome.TOKEN_REJECTED));

            verify(orcid, times(1)).create(any(), any(), any());
            verify(researcherRepository).markRevoked(ID, NOW);
            verify(entryRepository).recordFailure(eq(ID), eq(HANDLE), contains("access revoked"), eq(NOW));
        }

        @Test
        @DisplayName("Transient failure: retried up to the limit, then recorded as failed with the attempt count")
        void transientThenFailed() {
            authenticated(ID);
            when(orcid.create(any(), any(), any())).thenThrow(new OrcidWriteException(Kind.RETRYABLE, "ORCID returned 503"));

            assertThat(writer.sync(raid(contributor(ID, CO_INVESTIGATOR)))).extracting(EntryOutcome::outcome)
                    .containsExactly(Outcome.FAILED);

            verify(orcid, times(3)).create(any(), any(), any());
            verify(entryRepository).recordFailure(ID, HANDLE, "ORCID returned 503 (after 3 attempts)", NOW);
            verify(researcherRepository, never()).markRevoked(any(), any());
        }

        @Test
        @DisplayName("Transient failure that clears: written on a later attempt")
        void transientThenWritten() {
            authenticated(ID);
            when(orcid.create(any(), any(), any()))
                    .thenThrow(new OrcidWriteException(Kind.RETRYABLE, "ORCID returned 503"))
                    .thenReturn(99L);

            writer.sync(raid(contributor(ID, CO_INVESTIGATOR)));

            verify(entryRepository).recordSuccess(ID, HANDLE, 99L, NOW);
        }

        @Test
        @DisplayName("Refused (e.g. 409): recorded straight away, no retry")
        void refused() {
            authenticated(ID);
            when(orcid.create(any(), any(), any())).thenThrow(new OrcidWriteException(Kind.REJECTED, "ORCID returned 409"));

            writer.sync(raid(contributor(ID, CO_INVESTIGATOR)));

            verify(orcid, times(1)).create(any(), any(), any());
            verify(entryRepository).recordFailure(ID, HANDLE, "ORCID returned 409", NOW);
        }

        @Test
        @DisplayName("Stored token can't be decrypted: recorded, no ORCID call")
        void undecryptableToken() {
            when(researcherRepository.findAccessToken(ID)).thenReturn(Optional.of(tokenCipher.encrypt("t", OTHER_ID)));

            assertThat(writer.sync(raid(contributor(ID, CO_INVESTIGATOR)))).extracting(EntryOutcome::outcome)
                    .containsExactly(Outcome.FAILED);

            verifyNoInteractions(orcid);
            verify(entryRepository).recordFailure(eq(ID), eq(HANDLE), contains("can't be decrypted"), eq(NOW));
        }
    }

    @Nested
    @DisplayName("Removing contributors who left the RAiD")
    class Removing {
        @Test
        @DisplayName("Entry deleted at ORCID and forgotten; contributors still on the RAiD are untouched")
        void removed() {
            authenticated(OTHER_ID);
            when(entryRepository.findByRaidHandle(HANDLE)).thenReturn(List.of(
                    new OrcidEntry(ID, HANDLE, 1L), new OrcidEntry(OTHER_ID, HANDLE, 2L)));

            assertThat(writer.sync(raid(contributor(ID, CO_INVESTIGATOR))))
                    .contains(new EntryOutcome(OTHER_ID, Outcome.REMOVED));

            verify(orcid).delete(OTHER_ID, "token-" + OTHER_ID, 2L);
            verify(entryRepository).delete(OTHER_ID, HANDLE);
            verify(entryRepository, never()).delete(eq(ID), any());
        }

        @Test
        @DisplayName("Already gone at ORCID: forgotten")
        void alreadyGone() {
            authenticated(OTHER_ID);
            when(entryRepository.findByRaidHandle(HANDLE)).thenReturn(List.of(new OrcidEntry(OTHER_ID, HANDLE, 2L)));
            doThrow(new OrcidWriteException(Kind.NOT_FOUND, "404")).when(orcid).delete(any(), any(), anyLong());

            assertThat(writer.sync(raid())).containsExactly(new EntryOutcome(OTHER_ID, Outcome.REMOVED));
            verify(entryRepository).delete(OTHER_ID, HANDLE);
        }

        @Test
        @DisplayName("Never accepted by ORCID: forgotten without an ORCID call")
        void neverAccepted() {
            when(entryRepository.findByRaidHandle(HANDLE)).thenReturn(List.of(new OrcidEntry(OTHER_ID, HANDLE, null)));

            assertThat(writer.sync(raid())).containsExactly(new EntryOutcome(OTHER_ID, Outcome.REMOVED));
            verifyNoInteractions(orcid);
            verify(entryRepository).delete(OTHER_ID, HANDLE);
        }

        @Test
        @DisplayName("Researcher no longer authenticated: kept with a failure, so it can be removed later")
        void notAuthenticated() {
            when(entryRepository.findByRaidHandle(HANDLE)).thenReturn(List.of(new OrcidEntry(OTHER_ID, HANDLE, 2L)));

            assertThat(writer.sync(raid())).containsExactly(new EntryOutcome(OTHER_ID, Outcome.NOT_AUTHENTICATED));
            verifyNoInteractions(orcid);
            verify(entryRepository, never()).delete(any(), any());
            verify(entryRepository).recordFailure(eq(OTHER_ID), eq(HANDLE), anyString(), eq(NOW));
        }

        @Test
        @DisplayName("Removal fails: kept with the failure, for the next save to try again")
        void removalFails() {
            authenticated(OTHER_ID);
            when(entryRepository.findByRaidHandle(HANDLE)).thenReturn(List.of(new OrcidEntry(OTHER_ID, HANDLE, 2L)));
            doThrow(new OrcidWriteException(Kind.RETRYABLE, "ORCID returned 502")).when(orcid).delete(any(), any(), anyLong());

            assertThat(writer.sync(raid())).containsExactly(new EntryOutcome(OTHER_ID, Outcome.FAILED));
            verify(entryRepository, never()).delete(any(), any());
            verify(entryRepository).recordFailure(OTHER_ID, HANDLE, "ORCID returned 502 (after 3 attempts)", NOW);
        }
    }
}
