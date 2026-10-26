package au.org.raid.orcidupdater.invitation;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.crypto.EmailHasher;
import au.org.raid.orcidupdater.crypto.InvitationLinkToken;
import au.org.raid.orcidupdater.crypto.LinkCipher;
import au.org.raid.orcidupdater.raidapi.MinterNotPermittedException;
import au.org.raid.orcidupdater.raidapi.RaidApiClient;
import au.org.raid.orcidupdater.raidapi.RaidSummary;
import au.org.raid.orcidupdater.researcher.ResearcherRepository;
import au.org.raid.orcidupdater.status.ContributorStatus;
import au.org.raid.orcidupdater.support.TestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RAID-975 invitation scenarios, with the RAiD API, database and mail server stubbed. */
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("InvitationService")
class InvitationServiceTest {
    private static final String HANDLE = "10.82841/86bf1b6c";
    private static final String TYPED_ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String OTHER_ID = "https://orcid.org/0000-0001-5109-3700";
    private static final String EMAIL = " Researcher@Example.edu ";
    private static final String NORMALISED_EMAIL = "researcher@example.edu";
    private static final String MINTER_TOKEN = "minter-token";
    private static final Instant NOW = Instant.parse("2026-10-26T01:00:00Z");
    private static final RaidSummary RAID = new RaidSummary(HANDLE, "Coral reef resilience",
            "Reef Research Institute", List.of(TYPED_ID));

    private final RaidApiClient raidApiClient = mock(RaidApiClient.class);
    private final InvitationRepository invitationRepository = mock(InvitationRepository.class);
    private final ResearcherRepository researcherRepository = mock(ResearcherRepository.class);
    private final InvitationEmailSender emailSender = mock(InvitationEmailSender.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final OrcidUpdaterProperties properties = TestProperties.complete();
    private final EmailHasher emailHasher = new EmailHasher(properties);
    private final LinkCipher linkCipher = new LinkCipher(properties);

    private InvitationService service;

    @BeforeEach
    void setUp() {
        service = new InvitationService(raidApiClient, invitationRepository, researcherRepository, emailSender,
                emailHasher, linkCipher, properties, new TransactionTemplate(transactionManager),
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(raidApiClient.findEditableRaid(HANDLE, MINTER_TOKEN)).thenReturn(RAID);
        when(researcherRepository.findOrcidIdByConsentedEmailHash(any())).thenReturn(Optional.empty());
        when(invitationRepository.findPendingExpiry(anyString(), any())).thenReturn(Optional.empty());
    }

    private InvitationResponse register(final String contributorId) {
        return service.register(new InvitationRequest(HANDLE, contributorId, EMAIL), MINTER_TOKEN);
    }

    @Test
    @DisplayName("New contributor email: pending invitation with a single-use token, email sent to the normalised address")
    void newInvitation() {
        final var response = register(TYPED_ID);

        final var expiry = NOW.plus(Duration.ofDays(14));
        assertThat(response).isEqualTo(new InvitationResponse(TYPED_ID, ContributorStatus.AWAITING_AUTHENTICATION,
                true, expiry));

        final var tokenHash = ArgumentCaptor.forClass(byte[].class);
        verify(invitationRepository).insert(any(UUID.class), tokenHash.capture(), eq(TYPED_ID), eq(RAID),
                eq(NOW), eq(expiry));

        final var link = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(EMAIL), eq(RAID), link.capture(), eq(expiry));
        assertThat(link.getValue()).startsWith("https://app.example.org/invitations/");

        // the stored hash is of the token in the link, and the link carries the email fingerprint
        final var token = InvitationLinkToken.parse(link.getValue().substring(link.getValue().lastIndexOf('/') + 1))
                .orElseThrow();
        assertThat(token.lookupHash()).isEqualTo(tokenHash.getValue());
        assertThat(linkCipher.decrypt(token.sealedEmailHash(), token.random()))
                .contains(emailHasher.hash(NORMALISED_EMAIL));
    }

    @Test
    @DisplayName("The lock is taken before anything is checked or written")
    void locksFirst() {
        register(TYPED_ID);

        final var order = inOrder(invitationRepository, researcherRepository);
        order.verify(invitationRepository).lockOrcidId(TYPED_ID);
        order.verify(researcherRepository).findOrcidIdByConsentedEmailHash(any());
        order.verify(invitationRepository).insert(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Consented match: returns the authenticated iD, sends no email, records nothing")
    void consentedMatch() {
        when(researcherRepository.findOrcidIdByConsentedEmailHash(emailHasher.hash(NORMALISED_EMAIL)))
                .thenReturn(Optional.of(OTHER_ID));

        final var response = register(TYPED_ID);

        assertThat(response).isEqualTo(new InvitationResponse(OTHER_ID, ContributorStatus.AUTHENTICATED, false, null));
        verifyNoInteractions(emailSender);
        verify(invitationRepository, never()).insert(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Invitation already pending for this person: no second email, existing expiry returned")
    void alreadyPending() {
        final var existing = NOW.plus(Duration.ofDays(3));
        when(invitationRepository.findPendingExpiry(TYPED_ID, NOW)).thenReturn(Optional.of(existing));

        final var response = register(TYPED_ID);

        assertThat(response).isEqualTo(new InvitationResponse(TYPED_ID, ContributorStatus.AWAITING_AUTHENTICATION,
                false, existing));
        verifyNoInteractions(emailSender);
        verify(invitationRepository, never()).insert(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("iD not on the saved RAiD: refused before any lock, write or email")
    void notAContributor() {
        assertThatThrownBy(() -> register(OTHER_ID)).isInstanceOf(ContributorNotOnRaidException.class);

        verifyNoInteractions(invitationRepository, emailSender, transactionManager);
    }

    @Test
    @DisplayName("Minter can't edit the RAiD: refused before any lock, write or email")
    void notPermitted() {
        when(raidApiClient.findEditableRaid(HANDLE, MINTER_TOKEN)).thenThrow(new MinterNotPermittedException(HANDLE));

        assertThatThrownBy(() -> register(TYPED_ID)).isInstanceOf(MinterNotPermittedException.class);

        verifyNoInteractions(invitationRepository, emailSender, transactionManager);
    }

    @Test
    @DisplayName("Email cannot be sent: error identifies an email failure, the invitation is rolled back, no address logged")
    void emailFails(final CapturedOutput output) {
        doThrow(new EmailDeliveryException("Invitation email for " + HANDLE + " was not sent: MailSendException", null))
                .when(emailSender).send(any(), any(), any(), any());

        assertThatThrownBy(() -> register(TYPED_ID)).isInstanceOf(EmailDeliveryException.class);

        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
        assertThat(output.getAll()).doesNotContain(NORMALISED_EMAIL).doesNotContainIgnoringCase("Researcher@Example");
    }

    @Test
    @DisplayName("The email address never appears in the logs on success either")
    void successDoesNotLogEmail(final CapturedOutput output) {
        register(TYPED_ID);

        assertThat(output.getAll()).contains(TYPED_ID).doesNotContainIgnoringCase(NORMALISED_EMAIL);
    }

    @Test
    @DisplayName("A trailing slash on the public base URL does not double up in the link")
    void linkJoin() {
        register(TYPED_ID);

        final var link = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(any(), any(), link.capture(), any());
        assertThat(link.getValue()).doesNotContain(".org//");
    }
}
