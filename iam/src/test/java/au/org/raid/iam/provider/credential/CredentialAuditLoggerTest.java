package au.org.raid.iam.provider.credential;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.UserModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CredentialAuditLoggerTest {

    private static final String FIXED_NOW = "2026-08-27T00:00:00Z";

    @Mock private Logger auditLog;
    @Mock private UserModel actor;

    private CredentialAuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        when(actor.getId()).thenReturn("user-uuid");
        when(actor.getUsername()).thenReturn("sp-admin");
        auditLogger = new CredentialAuditLogger(
                Clock.fixed(Instant.parse(FIXED_NOW), ZoneOffset.UTC), auditLog);
    }

    private Object[] captureArgs() {
        final var args = ArgumentCaptor.forClass(Object[].class);
        verify(auditLog).info(org.mockito.ArgumentMatchers.anyString(), args.capture());
        return args.getValue();
    }

    @Test
    void recordsActorCredentialAndServicePoint() {
        auditLogger.record(CredentialAuditLogger.ACTION_REVEAL_SECRET, actor, "raid-cred-x", "group-a");

        final var args = captureArgs();
        assertThat(args, hasItemInArray("credential.reveal-secret"));
        assertThat(args, hasItemInArray("user-uuid"));
        assertThat(args, hasItemInArray("sp-admin"));
        assertThat(args, hasItemInArray("raid-cred-x"));
        assertThat(args, hasItemInArray("group-a"));
    }

    @Test
    void recordsTimestampAsIso8601Utc() {
        auditLogger.record(CredentialAuditLogger.ACTION_CREATE, actor, "raid-cred-x", "group-a");

        assertThat(captureArgs(), hasItemInArray(FIXED_NOW));
    }

    @Test
    void toleratesANullClientIdForActionsNotSpecificToOneCredential() {
        auditLogger.record(CredentialAuditLogger.ACTION_LIST, actor, null, "group-a");

        final var args = captureArgs();
        assertThat(args, hasItemInArray("credential.list"));
        assertThat(args, hasItemInArray("group-a"));
    }

    @Test
    void writesToTheDedicatedAuditLoggerName() {
        // Deployments route and retain the audit trail separately from application logging, so the
        // logger name is part of the contract.
        assertThat(CredentialAuditLogger.AUDIT_LOGGER_NAME, is("au.org.raid.iam.audit"));
    }

    @Test
    void recordsAtInfoAndNowhereElse() {
        // An audit trail suppressed by a production log level would be worthless, so it must be
        // INFO. captureArgs() verifies the info(..) call; verifyNoMoreInteractions then proves
        // nothing was written at debug or any other level instead.
        auditLogger.record(CredentialAuditLogger.ACTION_CREATE, actor, "raid-cred-x", "group-a");

        captureArgs();
        verifyNoMoreInteractions(auditLog);
    }
}
