package au.org.raid.orcidupdater.invitation;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.raidapi.RaidSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The invitation email (RAID-975): service point name, RAiD title and the link, and nothing that
 * identifies service point staff. Plain text, with a fixed subject so RAiD content can't reach
 * a mail header.
 */
@Component
@RequiredArgsConstructor
public class InvitationEmailSender {
    static final String SUBJECT = "Confirm your ORCID iD for a research project";
    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("en-AU")).withZone(ZoneOffset.UTC);

    private final JavaMailSender mailSender;
    private final OrcidUpdaterProperties properties;

    /** @throws EmailDeliveryException if the RA's mail server rejects or can't take the message */
    public void send(final String to, final RaidSummary raid, final String link, final Instant expiresAt) {
        final var message = new SimpleMailMessage();
        message.setFrom(properties.getMail().getFrom());
        message.setTo(to);
        message.setSubject(SUBJECT);
        message.setText(body(raid, link, expiresAt));

        try {
            mailSender.send(message);
        } catch (final MailException e) {
            // the exception's own message can include the recipient, so it is not passed on
            throw new EmailDeliveryException("Invitation email for %s was not sent: %s"
                    .formatted(raid.handle(), e.getClass().getSimpleName()), null);
        }
    }

    String body(final RaidSummary raid, final String link, final Instant expiresAt) {
        return """
                Hello,

                %s has named you as a contributor on the research project "%s". The project has a \
                Research Activity Identifier (RAiD).

                If you confirm your ORCID iD, %s can add this project to your ORCID record. You \
                decide whether to allow this.

                To confirm, open this link before %s:
                %s

                If you were not expecting this email, you can ignore it. Nothing is added to your \
                ORCID record unless you sign in to ORCID and give permission.
                """.formatted(raid.servicePointName(), raid.title(), properties.getOrcid().getClientName(),
                EXPIRY_FORMAT.format(expiresAt), link);
    }
}
