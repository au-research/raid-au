package au.org.raid.orcidupdater.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Plain SMTP from the RA's own settings, so mail sending is not tied to any cloud provider.
 * Built from {@link OrcidUpdaterProperties} rather than spring.mail.* so the host and sender are
 * validated with the other required settings.
 */
@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(final OrcidUpdaterProperties properties) {
        final var mail = properties.getMail();
        final var sender = new JavaMailSenderImpl();
        sender.setHost(mail.getHost());
        sender.setPort(mail.getPort());
        sender.setUsername(mail.getUsername());
        sender.setPassword(mail.getPassword());

        final var javaMailProperties = sender.getJavaMailProperties();
        javaMailProperties.put("mail.smtp.auth", String.valueOf(mail.getUsername() != null));
        javaMailProperties.put("mail.smtp.starttls.enable", String.valueOf(mail.isStartTls()));
        return sender;
    }
}
