package au.org.raid.orcidupdater.status;

import au.org.raid.orcidupdater.invitation.InvitationRepository;
import au.org.raid.orcidupdater.researcher.ResearcherRepository;
import au.org.raid.orcidupdater.status.ContributorStatusService.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ContributorStatusService")
class ContributorStatusServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-27T01:00:00Z");
    private static final String ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String OTHER_ID = "https://orcid.org/0000-0001-5109-3700";

    private final ResearcherRepository researcherRepository = mock(ResearcherRepository.class);
    private final InvitationRepository invitationRepository = mock(InvitationRepository.class);
    private final ContributorStatusService service = new ContributorStatusService(researcherRepository,
            invitationRepository, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        when(researcherRepository.findAccessToken(anyString())).thenReturn(Optional.empty());
        when(researcherRepository.isRevoked(anyString())).thenReturn(false);
        when(invitationRepository.findPendingExpiry(anyString(), any())).thenReturn(Optional.empty());
        when(invitationRepository.findOtherAuthenticatedOrcidId(anyString())).thenReturn(Optional.empty());
    }

    private void authenticated(final String id) {
        when(researcherRepository.findAccessToken(id)).thenReturn(Optional.of(new byte[]{1}));
    }

    private void pending(final String id) {
        when(invitationRepository.findPendingExpiry(id, NOW)).thenReturn(Optional.of(NOW.plus(Duration.ofDays(3))));
    }

    @Nested
    @DisplayName("Status of an iD")
    class StatusOf {
        @Test
        @DisplayName("Nothing held: UNAUTHENTICATED")
        void nothing() {
            assertThat(service.statusOf(ID)).isEqualTo(ContributorStatus.UNAUTHENTICATED);
        }

        @Test
        @DisplayName("Token held: AUTHENTICATED, even with a pending invitation")
        void authenticatedWins() {
            authenticated(ID);
            pending(ID);

            assertThat(service.statusOf(ID)).isEqualTo(ContributorStatus.AUTHENTICATED);
        }

        @Test
        @DisplayName("Pending invitation: AWAITING_AUTHENTICATION")
        void awaiting() {
            pending(ID);

            assertThat(service.statusOf(ID)).isEqualTo(ContributorStatus.AWAITING_AUTHENTICATION);
        }

        @Test
        @DisplayName("Revoked and invited again: AWAITING_AUTHENTICATION, not revoked")
        void revokedThenInvited() {
            when(researcherRepository.isRevoked(ID)).thenReturn(true);
            pending(ID);

            assertThat(service.statusOf(ID)).isEqualTo(ContributorStatus.AWAITING_AUTHENTICATION);
        }

        @Test
        @DisplayName("Revoked, nothing pending: AUTHENTICATION_REVOKED")
        void revoked() {
            when(researcherRepository.isRevoked(ID)).thenReturn(true);

            assertThat(service.statusOf(ID)).isEqualTo(ContributorStatus.AUTHENTICATION_REVOKED);
        }
    }

    @Nested
    @DisplayName("Lookup for the RAiD API")
    class Lookup {
        @Test
        @DisplayName("Authenticated iD: AUTHENTICATED with itself as the authenticated iD")
        void self() {
            authenticated(ID);

            assertThat(service.lookup(ID)).isEqualTo(new Status(ID, ContributorStatus.AUTHENTICATED, ID));
        }

        @Test
        @DisplayName("Invitation used to sign in as another, authenticated iD: AUTHENTICATED with that iD")
        void signedInAsOther() {
            when(invitationRepository.findOtherAuthenticatedOrcidId(ID)).thenReturn(Optional.of(OTHER_ID));
            authenticated(OTHER_ID);

            assertThat(service.lookup(ID)).isEqualTo(new Status(ID, ContributorStatus.AUTHENTICATED, OTHER_ID));
        }

        @Test
        @DisplayName("The other iD's token has since been revoked: the typed iD's own status, no authenticated iD")
        void otherRevoked() {
            when(invitationRepository.findOtherAuthenticatedOrcidId(ID)).thenReturn(Optional.of(OTHER_ID));

            assertThat(service.lookup(ID)).isEqualTo(new Status(ID, ContributorStatus.UNAUTHENTICATED, null));
        }

        @Test
        @DisplayName("Pending invitation: AWAITING_AUTHENTICATION, no authenticated iD")
        void awaiting() {
            pending(ID);

            assertThat(service.lookup(ID)).isEqualTo(new Status(ID, ContributorStatus.AWAITING_AUTHENTICATION, null));
        }
    }
}
