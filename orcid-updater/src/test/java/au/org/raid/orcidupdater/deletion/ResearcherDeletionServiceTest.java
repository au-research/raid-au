package au.org.raid.orcidupdater.deletion;

import au.org.raid.orcidupdater.crypto.EmailHasher;
import au.org.raid.orcidupdater.crypto.TokenCipher;
import au.org.raid.orcidupdater.invitation.InvitationRepository;
import au.org.raid.orcidupdater.orcid.OrcidEntryRepository;
import au.org.raid.orcidupdater.orcid.OrcidEntryRepository.OrcidEntry;
import au.org.raid.orcidupdater.orcid.OrcidMemberClient;
import au.org.raid.orcidupdater.orcid.OrcidWriteException;
import au.org.raid.orcidupdater.orcid.OrcidWriteException.Kind;
import au.org.raid.orcidupdater.researcher.ResearcherRepository;
import au.org.raid.orcidupdater.signin.OAuthStateRepository;
import au.org.raid.orcidupdater.signin.OrcidOAuthClient;
import au.org.raid.orcidupdater.support.TestProperties;
import au.org.raid.orcidupdater.work.WorkQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RAID-1006 scenarios, with ORCID and the database stubbed. */
@DisplayName("ResearcherDeletionService")
class ResearcherDeletionServiceTest {
    private static final String ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String TYPED_ID = "https://orcid.org/0000-0001-5109-3700";
    private static final String EMAIL = "researcher@example.edu";
    private static final String TOKEN = "token-" + ID;

    private final ResearcherRepository researcherRepository = mock(ResearcherRepository.class);
    private final OrcidEntryRepository entryRepository = mock(OrcidEntryRepository.class);
    private final InvitationRepository invitationRepository = mock(InvitationRepository.class);
    private final OAuthStateRepository oauthStateRepository = mock(OAuthStateRepository.class);
    private final OrcidMemberClient orcid = mock(OrcidMemberClient.class);
    private final OrcidOAuthClient oauth = mock(OrcidOAuthClient.class);
    private final WorkQueue workQueue = mock(WorkQueue.class);
    private final TokenCipher tokenCipher = new TokenCipher(TestProperties.complete());
    private final EmailHasher emailHasher = new EmailHasher(TestProperties.complete());

    private ResearcherDeletionService service;

    @BeforeEach
    void setUp() {
        service = new ResearcherDeletionService(researcherRepository, entryRepository, invitationRepository,
                oauthStateRepository, orcid, oauth, tokenCipher, emailHasher, workQueue,
                new TransactionTemplate(mock(PlatformTransactionManager.class)));
        when(researcherRepository.findOrcidIdByEmailHash(any())).thenReturn(Optional.empty());
        when(researcherRepository.findAccessToken(anyString())).thenReturn(Optional.empty());
        when(entryRepository.findByOrcidId(anyString())).thenReturn(List.of());
        when(invitationRepository.deleteForOrcidId(anyString())).thenReturn(List.of());
    }

    private void authenticated() {
        when(researcherRepository.findAccessToken(ID)).thenReturn(Optional.of(tokenCipher.encrypt(TOKEN, ID)));
        when(researcherRepository.delete(ID)).thenReturn(true);
    }

    private void entries(final Long... putCodes) {
        when(entryRepository.findByOrcidId(ID)).thenReturn(java.util.Arrays.stream(putCodes)
                .map(putCode -> new OrcidEntry(ID, "10.82841/" + putCode, putCode)).toList());
    }

    private static ResearcherDeletionRequest byId() {
        return new ResearcherDeletionRequest(null, ID);
    }

    @Nested
    @DisplayName("Authenticated researcher")
    class Authenticated {
        @Test
        @DisplayName("Removes each ORCID entry, revokes the token, then deletes everything held, under the iD's lock")
        void deletesInOrder() {
            authenticated();
            entries(11L, 12L);
            when(invitationRepository.deleteForOrcidId(ID)).thenReturn(List.of(ID));

            final var response = service.delete(byId());

            assertThat(response).isEqualTo(new ResearcherDeletionResponse(true, 2, 0, true));
            final InOrder order = inOrder(invitationRepository, orcid, oauth, entryRepository, researcherRepository);
            order.verify(invitationRepository).lockOrcidId(ID);
            order.verify(orcid).delete(ID, TOKEN, 11L);
            order.verify(orcid).delete(ID, TOKEN, 12L);
            order.verify(oauth).revoke(TOKEN);
            order.verify(entryRepository).deleteByOrcidId(ID);
            order.verify(invitationRepository).deleteForOrcidId(ID);
            order.verify(researcherRepository).delete(ID);
            verify(workQueue).requestStatus(ID);
        }

        @Test
        @DisplayName("Entry already gone at ORCID (404): counted as removed")
        void alreadyGone() {
            authenticated();
            entries(11L);
            doThrow(new OrcidWriteException(Kind.NOT_FOUND, "ORCID returned 404")).when(orcid).delete(ID, TOKEN, 11L);

            assertThat(service.delete(byId())).isEqualTo(new ResearcherDeletionResponse(true, 1, 0, true));
        }

        @Test
        @DisplayName("Entry never accepted by ORCID (no put code): nothing to remove there, row still deleted")
        void noPutCode() {
            authenticated();
            when(entryRepository.findByOrcidId(ID)).thenReturn(List.of(new OrcidEntry(ID, "10.82841/1", null)));

            assertThat(service.delete(byId())).isEqualTo(new ResearcherDeletionResponse(true, 0, 0, true));
            verify(orcid, never()).delete(anyString(), anyString(), anyLong());
            verify(entryRepository).deleteByOrcidId(ID);
        }

        @Test
        @DisplayName("ORCID refuses one removal (e.g. 409): that entry is reported left and the rest carries on")
        void refusedRemoval() {
            authenticated();
            entries(11L, 12L);
            doThrow(new OrcidWriteException(Kind.REJECTED, "ORCID returned 409")).when(orcid).delete(ID, TOKEN, 11L);

            assertThat(service.delete(byId())).isEqualTo(new ResearcherDeletionResponse(true, 1, 1, true));
            verify(researcherRepository).delete(ID);
        }

        @Test
        @DisplayName("Typed iDs from invitations the researcher used get their status pushed too")
        void typedIds() {
            authenticated();
            when(invitationRepository.deleteForOrcidId(ID)).thenReturn(List.of(TYPED_ID, TYPED_ID, ID));

            service.delete(byId());

            verify(workQueue).requestStatus(ID);
            verify(workQueue).requestStatus(TYPED_ID);
        }
    }

    @Nested
    @DisplayName("ORCID fails: nothing here is deleted, so the administrator can try again")
    class OrcidFails {
        @Test
        @DisplayName("ORCID unavailable while removing an entry")
        void removalUnavailable() {
            authenticated();
            entries(11L);
            doThrow(new OrcidWriteException(Kind.RETRYABLE, "ORCID returned 503")).when(orcid).delete(ID, TOKEN, 11L);

            assertThatThrownBy(() -> service.delete(byId())).isInstanceOf(OrcidUnavailableException.class)
                    .hasMessageContaining("503").hasMessageNotContaining(TOKEN);
            verify(oauth, never()).revoke(anyString());
            verify(researcherRepository, never()).delete(anyString());
            verify(entryRepository, never()).deleteByOrcidId(anyString());
        }

        @Test
        @DisplayName("ORCID refuses or fails the revocation")
        void revokeFails() {
            authenticated();
            for (final var kind : List.of(Kind.RETRYABLE, Kind.REJECTED)) {
                doThrow(new OrcidWriteException(kind, "ORCID token revocation returned 400")).when(oauth).revoke(TOKEN);

                assertThatThrownBy(() -> service.delete(byId())).isInstanceOf(OrcidUnavailableException.class);
            }
            verify(researcherRepository, never()).delete(anyString());
            verify(invitationRepository, never()).deleteForOrcidId(anyString());
            verifyNoInteractions(workQueue);
        }
    }

    @Nested
    @DisplayName("ORCID no longer accepts the token")
    class TokenUnusable {
        @Test
        @DisplayName("Token rejected (401): remaining entries left, no revocation, data still deleted")
        void tokenRejected() {
            authenticated();
            entries(11L, 12L);
            doThrow(new OrcidWriteException(Kind.TOKEN_REJECTED, "ORCID returned 401")).when(orcid).delete(ID, TOKEN, 11L);

            assertThat(service.delete(byId())).isEqualTo(new ResearcherDeletionResponse(true, 0, 2, false));
            verify(orcid, never()).delete(ID, TOKEN, 12L);
            verify(oauth, never()).revoke(anyString());
            verify(researcherRepository).delete(ID);
        }

        @Test
        @DisplayName("Revoked earlier, or the token can't be decrypted: no ORCID calls, entries left, data deleted")
        void noUsableToken() {
            when(researcherRepository.delete(ID)).thenReturn(true);
            entries(11L);

            assertThat(service.delete(byId())).isEqualTo(new ResearcherDeletionResponse(true, 0, 1, false));

            when(researcherRepository.findAccessToken(ID)).thenReturn(Optional.of(new byte[]{1, 2, 3}));
            assertThat(service.delete(byId())).isEqualTo(new ResearcherDeletionResponse(true, 0, 1, false));
            verifyNoInteractions(orcid, oauth);
        }
    }

    @Nested
    @DisplayName("Finding the researcher")
    class Finding {
        @Test
        @DisplayName("By email: the researcher who consented with it, and sign-ins in progress carrying it")
        void byEmail() {
            authenticated();
            final var hash = emailHasher.hash(EMAIL);
            when(researcherRepository.findOrcidIdByEmailHash(hash)).thenReturn(Optional.of(ID));

            assertThat(service.delete(new ResearcherDeletionRequest("Researcher@Example.edu", null)).found()).isTrue();

            verify(oauthStateRepository).deleteByEmailHash(hash);
            verify(researcherRepository).delete(ID);
        }

        @Test
        @DisplayName("By email held nowhere but a sign-in in progress: that is dropped and reported found")
        void emailOnlyInSignIn() {
            when(oauthStateRepository.deleteByEmailHash(any())).thenReturn(1);

            assertThat(service.delete(new ResearcherDeletionRequest(EMAIL, null)))
                    .isEqualTo(new ResearcherDeletionResponse(true, 0, 0, false));
            verify(invitationRepository, never()).lockOrcidId(anyString());
        }

        @Test
        @DisplayName("Nothing held: found false, nothing pushed")
        void nothingHeld() {
            assertThat(service.delete(new ResearcherDeletionRequest(EMAIL, null)))
                    .isEqualTo(ResearcherDeletionResponse.NOTHING_HELD);
            assertThat(service.delete(byId())).isEqualTo(ResearcherDeletionResponse.NOTHING_HELD);
            verifyNoInteractions(workQueue, orcid, oauth);
        }

        @Test
        @DisplayName("Email and iD belong to different researchers: refused, nothing deleted")
        void mismatch() {
            when(researcherRepository.findOrcidIdByEmailHash(any())).thenReturn(Optional.of(TYPED_ID));

            assertThatThrownBy(() -> service.delete(new ResearcherDeletionRequest(EMAIL, ID)))
                    .isInstanceOf(ResearcherMismatchException.class);
            verify(researcherRepository, never()).delete(anyString());
            verifyNoInteractions(oauthStateRepository, orcid, oauth);
        }

        @Test
        @DisplayName("Email and iD of the same researcher, or an email nobody consented with: deleted by the iD")
        void emailAndId() {
            authenticated();
            when(researcherRepository.findOrcidIdByEmailHash(any())).thenReturn(Optional.of(ID));
            assertThat(service.delete(new ResearcherDeletionRequest(EMAIL, ID)).found()).isTrue();

            when(researcherRepository.findOrcidIdByEmailHash(any())).thenReturn(Optional.empty());
            assertThat(service.delete(new ResearcherDeletionRequest(EMAIL, ID)).found()).isTrue();
            verify(oauthStateRepository, org.mockito.Mockito.times(2)).deleteByEmailHash(eq(emailHasher.hash(EMAIL)));
        }
    }
}
