package au.org.raid.orcidupdater.invitation;

import au.org.raid.orcidupdater.raidapi.RaidSummary;
import au.org.raid.orcidupdater.support.TestProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("InvitationEmailSender")
class InvitationEmailSenderTest {
    private static final String TO = "researcher@example.edu";
    private static final String LINK = "https://orcid-updater.example.org/invitations/abc.def";
    private static final Instant EXPIRES = Instant.parse("2026-11-09T03:00:00Z");
    private static final RaidSummary RAID = new RaidSummary("10.82841/86bf1b6c", "Coral reef resilience",
            "Reef Research Institute", List.of());

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final InvitationEmailSender sender = new InvitationEmailSender(mailSender, TestProperties.complete());

    @Test
    @DisplayName("Sends from the RA's address with the service point name, RAiD title, client name, link and expiry")
    void sends() {
        sender.send(TO, RAID, LINK, EXPIRES);

        final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        final var message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("orcid-updater@example.org");
        assertThat(message.getTo()).containsExactly(TO);
        assertThat(message.getSubject()).isEqualTo(InvitationEmailSender.SUBJECT);
        assertThat(message.getText()).contains("Reef Research Institute", "\"Coral reef resilience\"",
                "ARDC RAiD Service", LINK, "9 November 2026");
    }

    @Test
    @DisplayName("RAiD content never reaches the subject line")
    void fixedSubject() {
        final var hostile = new RaidSummary("10.1/x", "Title\r\nBcc: someone@example.org", "SP", List.of());
        sender.send(TO, hostile, LINK, EXPIRES);

        final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo(InvitationEmailSender.SUBJECT);
        assertThat(captor.getValue().getBcc()).isNull();
    }

    @Test
    @DisplayName("Mail server failure: reported without the address")
    void failureHidesAddress() {
        doThrow(new MailSendException("550 rejected recipient " + TO)).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> sender.send(TO, RAID, LINK, EXPIRES))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("10.82841/86bf1b6c")
                .hasMessageNotContaining(TO)
                .hasNoCause();
    }
}
