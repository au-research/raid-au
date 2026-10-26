package au.org.raid.orcidupdater.signin;

import au.org.raid.orcidupdater.crypto.EmailHasher;
import au.org.raid.orcidupdater.crypto.InvitationLinkToken;
import au.org.raid.orcidupdater.crypto.LinkCipher;
import au.org.raid.orcidupdater.crypto.TokenCipher;
import au.org.raid.orcidupdater.invitation.Invitation;
import au.org.raid.orcidupdater.invitation.InvitationRepository;
import au.org.raid.orcidupdater.researcher.ResearcherRepository;
import au.org.raid.orcidupdater.signin.InvalidInvitationLinkException.Reason;
import au.org.raid.orcidupdater.signin.OAuthStateRepository.OAuthState;
import au.org.raid.orcidupdater.signin.OrcidOAuthClient.OrcidSignIn;
import au.org.raid.orcidupdater.signin.SignInService.Outcome;
import au.org.raid.orcidupdater.support.TestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RAID-975 researcher scenarios, with the database and ORCID stubbed. */
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("SignInService")
class SignInServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-26T01:00:00Z");
    private static final String TYPED_ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String OTHER_ID = "https://sandbox.orcid.org/0000-0001-5109-3700";
    private static final String EMAIL = "researcher@example.edu";
    private static final URI ORCID_AUTHORISE = URI.create("https://sandbox.orcid.org/oauth/authorize?state=x");

    private final InvitationRepository invitationRepository = mock(InvitationRepository.class);
    private final OAuthStateRepository oauthStateRepository = mock(OAuthStateRepository.class);
    private final ResearcherRepository researcherRepository = mock(ResearcherRepository.class);
    private final OrcidOAuthClient orcidOAuthClient = mock(OrcidOAuthClient.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final LinkCipher linkCipher = new LinkCipher(TestProperties.complete());
    private final TokenCipher tokenCipher = new TokenCipher(TestProperties.complete());
    private final byte[] emailHash = new EmailHasher(TestProperties.complete()).hash(EMAIL);
    private final InvitationLinkToken linkToken = InvitationLinkToken.issue(emailHash, linkCipher);
    private final Invitation pending = invitation(null, NOW.plus(Duration.ofDays(14)));

    private SignInService service;

    private static Invitation invitation(final Instant usedAt, final Instant expiresAt) {
        return new Invitation(UUID.fromString("5c8a3f1e-8d55-4c3a-9a0e-2f4b6f2b1c11"), TYPED_ID, "10.82841/86bf1b6c",
                "Coral reef resilience", "Reef Research Institute", expiresAt, usedAt);
    }

    private static byte[] sha256(final String value) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() {
        service = new SignInService(invitationRepository, oauthStateRepository, researcherRepository, orcidOAuthClient,
                linkCipher, tokenCipher, new TransactionTemplate(transactionManager), Clock.fixed(NOW, ZoneOffset.UTC));
        when(invitationRepository.findByTokenHash(linkToken.lookupHash())).thenReturn(Optional.of(pending));
        when(invitationRepository.findById(pending.id())).thenReturn(Optional.of(pending));
        when(orcidOAuthClient.authorisationUri(anyString())).thenReturn(ORCID_AUTHORISE);
    }

    @Nested
    @DisplayName("Opening the invitation link")
    class Open {
        @Test
        @DisplayName("Valid link: the invitation, for the landing page")
        void valid() {
            assertThat(service.openInvitation(linkToken.encode())).isEqualTo(pending);
        }

        @Test
        @DisplayName("Malformed or unknown link: UNKNOWN")
        void unknown() {
            assertReason(() -> service.openInvitation("not-a-token"), Reason.UNKNOWN);
            final var other = InvitationLinkToken.issue(emailHash, linkCipher);
            when(invitationRepository.findByTokenHash(other.lookupHash())).thenReturn(Optional.empty());
            assertReason(() -> service.openInvitation(other.encode()), Reason.UNKNOWN);
        }

        @Test
        @DisplayName("Already used: USED")
        void used() {
            when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation(NOW, NOW.plusSeconds(60))));
            assertReason(() -> service.openInvitation(linkToken.encode()), Reason.USED);
        }

        @Test
        @DisplayName("Expired, including at the exact expiry time: EXPIRED")
        void expired() {
            when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation(null, NOW)));
            assertReason(() -> service.openInvitation(linkToken.encode()), Reason.EXPIRED);
        }
    }

    @Nested
    @DisplayName("Starting ORCID sign-in")
    class Start {
        @Test
        @DisplayName("Without Remember me: a state is stored hashed with no email fingerprint, and ORCID's URL returned")
        void withoutRememberMe() throws Exception {
            assertThat(service.startSignIn(linkToken.encode(), false)).isEqualTo(ORCID_AUTHORISE);

            final var state = ArgumentCaptor.forClass(String.class);
            verify(orcidOAuthClient).authorisationUri(state.capture());
            verify(oauthStateRepository).insert(eq(sha256(state.getValue())), eq(pending.id()), isNull(), eq(NOW),
                    eq(NOW.plus(SignInService.SIGN_IN_TIMEOUT)));
        }

        @Test
        @DisplayName("With Remember me: the fingerprint from the link is held with the state")
        void withRememberMe() {
            service.startSignIn(linkToken.encode(), true);

            verify(oauthStateRepository).insert(any(), eq(pending.id()), eq(emailHash), any(), any());
        }

        @Test
        @DisplayName("Each click gets a different state")
        void freshState() {
            service.startSignIn(linkToken.encode(), false);
            service.startSignIn(linkToken.encode(), false);

            final var states = ArgumentCaptor.forClass(String.class);
            verify(orcidOAuthClient, org.mockito.Mockito.times(2)).authorisationUri(states.capture());
            assertThat(states.getAllValues().get(0)).isNotEqualTo(states.getAllValues().get(1)).hasSizeGreaterThan(40);
        }

        @Test
        @DisplayName("Remember me with an altered fingerprint: treated as an unknown link, nothing stored")
        void tamperedFingerprint() {
            final var sealed = linkToken.sealedEmailHash().clone();
            sealed[sealed.length - 1] ^= 1;
            final var altered = new InvitationLinkToken(linkToken.random(), sealed).encode();

            assertReason(() -> service.startSignIn(altered, true), Reason.UNKNOWN);
            verifyNoInteractions(oauthStateRepository);
        }

        @Test
        @DisplayName("Used or expired link: never sent to ORCID")
        void invalidLinkNotRedirected() {
            when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation(NOW, NOW.plusSeconds(60))));

            assertReason(() -> service.startSignIn(linkToken.encode(), false), Reason.USED);
            verifyNoInteractions(oauthStateRepository);
            verify(orcidOAuthClient, never()).authorisationUri(any());
        }
    }

    @Nested
    @DisplayName("ORCID callback")
    class Callback {
        private static final String STATE = "state-value";
        private static final String CODE = "Q70Y3A";
        private static final OrcidSignIn SIGN_IN =
                new OrcidSignIn(TYPED_ID, "access-token-value", "refresh-token-value", "/read-limited /activities/update");

        private void attempt(final byte[] withEmailHash, final Instant expiresAt) throws Exception {
            when(oauthStateRepository.consume(sha256(STATE)))
                    .thenReturn(Optional.of(new OAuthState(pending.id(), withEmailHash, expiresAt)));
        }

        @BeforeEach
        void setUp() throws Exception {
            attempt(null, NOW.plus(Duration.ofMinutes(10)));
            when(invitationRepository.markUsed(any(), any(), any())).thenReturn(true);
            when(orcidOAuthClient.exchange(CODE)).thenReturn(SIGN_IN);
        }

        @Test
        @DisplayName("Signed in without Remember me: token stored encrypted, link used up, no fingerprint")
        void authenticatedWithoutRememberMe() {
            final var result = service.completeSignIn(STATE, CODE, null);

            assertThat(result.outcome()).isEqualTo(Outcome.AUTHENTICATED);
            assertThat(result.orcidId()).isEqualTo(TYPED_ID);
            assertThat(result.remembered()).isFalse();

            verify(invitationRepository).lockOrcidId(TYPED_ID);
            verify(invitationRepository).markUsed(pending.id(), TYPED_ID, NOW);
            final var access = ArgumentCaptor.forClass(byte[].class);
            final var refresh = ArgumentCaptor.forClass(byte[].class);
            verify(researcherRepository).saveAuthentication(eq(TYPED_ID), access.capture(), refresh.capture(),
                    eq("/read-limited /activities/update"), eq(NOW));
            assertThat(tokenCipher.decrypt(access.getValue(), TYPED_ID)).isEqualTo("access-token-value");
            assertThat(tokenCipher.decrypt(refresh.getValue(), TYPED_ID)).isEqualTo("refresh-token-value");
            verify(researcherRepository, never()).recordConsent(any(), any(), any());
        }

        @Test
        @DisplayName("Signed in with Remember me: fingerprint and consent time stored for the authenticated iD")
        void authenticatedWithRememberMe() throws Exception {
            attempt(emailHash, NOW.plus(Duration.ofMinutes(10)));

            assertThat(service.completeSignIn(STATE, CODE, null).remembered()).isTrue();

            verify(researcherRepository).recordConsent(TYPED_ID, emailHash, NOW);
        }

        @Test
        @DisplayName("ORCID returns a different iD: the authenticated iD is recorded, not the typed one")
        void differentId() throws Exception {
            attempt(emailHash, NOW.plus(Duration.ofMinutes(10)));
            when(orcidOAuthClient.exchange(CODE)).thenReturn(new OrcidSignIn(OTHER_ID, "a", "r", "s"));

            final var result = service.completeSignIn(STATE, CODE, null);

            assertThat(result.orcidId()).isEqualTo(OTHER_ID);
            assertThat(result.typedOrcidId()).isEqualTo(TYPED_ID);
            verify(invitationRepository).markUsed(pending.id(), OTHER_ID, NOW);
            verify(researcherRepository).saveAuthentication(eq(OTHER_ID), any(), any(), any(), any());
            verify(researcherRepository).recordConsent(OTHER_ID, emailHash, NOW);
        }

        @Test
        @DisplayName("Researcher declines at ORCID: nothing stored, no exchange, link not used up")
        void declined() {
            final var result = service.completeSignIn(STATE, null, "access_denied");

            assertThat(result.outcome()).isEqualTo(Outcome.DECLINED);
            assertThat(result.orcidId()).isNull();
            assertThat(result.invitationExpiresAt()).isEqualTo(pending.expiresAt());
            verify(orcidOAuthClient, never()).exchange(any());
            verify(invitationRepository, never()).markUsed(any(), any(), any());
            verifyNoInteractions(researcherRepository, transactionManager);
        }

        @Test
        @DisplayName("Unknown or replayed state: rejected before anything else")
        void unknownState() {
            when(oauthStateRepository.consume(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.completeSignIn("forged", CODE, null)).isInstanceOf(InvalidOAuthStateException.class);
            verify(orcidOAuthClient, never()).exchange(any());
            verifyNoInteractions(researcherRepository);
        }

        @Test
        @DisplayName("Missing state: rejected without a lookup")
        void missingState() {
            assertThatThrownBy(() -> service.completeSignIn(null, CODE, null)).isInstanceOf(InvalidOAuthStateException.class);
            verifyNoInteractions(oauthStateRepository, researcherRepository);
        }

        @Test
        @DisplayName("State older than the sign-in timeout: rejected, no exchange")
        void expiredState() throws Exception {
            attempt(null, NOW);

            assertThatThrownBy(() -> service.completeSignIn(STATE, CODE, null)).isInstanceOf(InvalidOAuthStateException.class);
            verify(orcidOAuthClient, never()).exchange(any());
        }

        @Test
        @DisplayName("Neither code nor error: rejected, no exchange")
        void noCode() {
            assertThatThrownBy(() -> service.completeSignIn(STATE, " ", null)).isInstanceOf(InvalidOAuthStateException.class);
            verify(orcidOAuthClient, never()).exchange(any());
        }

        @Test
        @DisplayName("Invitation used while the researcher was at ORCID: USED, no exchange")
        void usedMeanwhile() {
            when(invitationRepository.findById(pending.id())).thenReturn(Optional.of(invitation(NOW, NOW.plusSeconds(60))));

            assertReason(() -> service.completeSignIn(STATE, CODE, null), Reason.USED);
            verify(orcidOAuthClient, never()).exchange(any());
        }

        @Test
        @DisplayName("Invitation expired while the researcher was at ORCID: EXPIRED, no exchange")
        void expiredMeanwhile() {
            when(invitationRepository.findById(pending.id())).thenReturn(Optional.of(invitation(null, NOW)));

            assertReason(() -> service.completeSignIn(STATE, CODE, null), Reason.EXPIRED);
            verify(orcidOAuthClient, never()).exchange(any());
        }

        @Test
        @DisplayName("Another callback used the link first: USED, the token is not kept, the transaction rolls back")
        void lostRace() {
            when(invitationRepository.markUsed(any(), any(), any())).thenReturn(false);

            assertReason(() -> service.completeSignIn(STATE, CODE, null), Reason.USED);
            verify(researcherRepository, never()).saveAuthentication(any(), any(), any(), any(), any());
            verify(transactionManager).rollback(any());
        }

        @Test
        @DisplayName("ORCID won't exchange the code: the error passes up and nothing is stored")
        void exchangeFails() {
            when(orcidOAuthClient.exchange(CODE)).thenThrow(new OrcidSignInFailedException("ORCID token exchange returned 400"));

            assertThatThrownBy(() -> service.completeSignIn(STATE, CODE, null)).isInstanceOf(OrcidSignInFailedException.class);
            verifyNoInteractions(researcherRepository);
            verify(invitationRepository, never()).markUsed(any(), any(), any());
        }

        @Test
        @DisplayName("Logs never contain the state, the code or the ORCID tokens")
        void nothingSecretLogged(final CapturedOutput output) {
            service.completeSignIn(STATE, CODE, null);

            assertThat(output.getAll()).contains(TYPED_ID)
                    .doesNotContain(STATE, CODE, "access-token-value", "refresh-token-value");
        }

        @Test
        @DisplayName("An error value from the query string is only logged if it looks like an OAuth error code")
        void errorValueSanitised(final CapturedOutput output) {
            service.completeSignIn(STATE, null, "access_denied\nFAKE LOG LINE");

            assertThat(output.getAll()).doesNotContain("FAKE LOG LINE").contains("an unrecognised error");
        }
    }

    private static void assertReason(final org.assertj.core.api.ThrowableAssert.ThrowingCallable call, final Reason reason) {
        assertThatThrownBy(call).isInstanceOfSatisfying(InvalidInvitationLinkException.class,
                e -> assertThat(e.getReason()).isEqualTo(reason));
    }
}
