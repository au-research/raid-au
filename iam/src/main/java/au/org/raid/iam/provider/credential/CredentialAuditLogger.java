package au.org.raid.iam.provider.credential;

import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Audit trail for client credential lifecycle actions (RAID-847).
 *
 * <p>Records who did what, when, and to which credential. It deliberately has no parameter capable
 * of carrying a secret value, so the type itself guarantees a secret cannot be audited by mistake.
 *
 * <p>Entries are written at INFO under the dedicated logger name {@value #AUDIT_LOGGER_NAME} rather
 * than this class's own name, so deployments can route or retain the audit trail independently of
 * ordinary application logging.
 */
public class CredentialAuditLogger {
    static final String AUDIT_LOGGER_NAME = "au.org.raid.iam.audit";

    public static final String ACTION_CREATE = "credential.create";
    public static final String ACTION_LIST = "credential.list";
    public static final String ACTION_ROTATE = "credential.rotate";
    public static final String ACTION_REVOKE = "credential.revoke";
    public static final String ACTION_REVEAL_SECRET = "credential.reveal-secret";

    private final Logger auditLog;
    private final Clock clock;

    public CredentialAuditLogger(final Clock clock) {
        this(clock, LoggerFactory.getLogger(AUDIT_LOGGER_NAME));
    }

    // Package-private seam for tests, so the audit output can be captured without a logging appender.
    CredentialAuditLogger(final Clock clock, final Logger auditLog) {
        this.clock = clock;
        this.auditLog = auditLog;
    }

    /**
     * @param action   one of the {@code ACTION_*} constants
     * @param actor    the authenticated caller
     * @param clientId the credential acted on, or null where the action is not specific to one
     *                 (a list, for instance)
     * @param groupId  the owning service point group
     */
    public void record(final String action, final UserModel actor, final String clientId, final String groupId) {
        auditLog.info("action={} at={} actorId={} actor={} clientId={} servicePointGroupId={}",
                action,
                DateTimeFormatter.ISO_INSTANT.format(Instant.now(clock).truncatedTo(ChronoUnit.SECONDS)),
                actor == null ? null : actor.getId(),
                actor == null ? null : actor.getUsername(),
                clientId,
                groupId);
    }
}
