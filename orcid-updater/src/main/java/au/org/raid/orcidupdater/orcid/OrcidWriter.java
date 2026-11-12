package au.org.raid.orcidupdater.orcid;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.crypto.TokenCipher;
import au.org.raid.orcidupdater.invitation.InvitationRepository;
import au.org.raid.orcidupdater.orcid.OrcidEntryRepository.OrcidEntry;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Contributor;
import au.org.raid.orcidupdater.orcid.OrcidWriteException.Kind;
import au.org.raid.orcidupdater.researcher.ResearcherRepository;
import au.org.raid.orcidupdater.work.WorkQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Keeps a RAiD's entries on its contributors' ORCID records in step with the RAiD (RAID-1004):
 * writes an entry for each authenticated contributor and removes the entries of contributors no
 * longer on it. Called by RAID-1005 when a RAiD changes or a contributor's status does.
 *
 * <p>Each researcher's entry is written while holding that iD's advisory lock, so two syncs of
 * the same RAiD can't both create an entry. Failures are recorded on the entry for operators;
 * nothing retries in the background (Decision 2026-10-23, Q6).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrcidWriter {
    public enum Outcome {
        WRITTEN,
        REMOVED,
        /** No ORCID call: the researcher hasn't authenticated, or revoked access earlier. */
        NOT_AUTHENTICATED,
        /** ORCID rejected the token just now; the researcher is marked revoked. */
        TOKEN_REJECTED,
        FAILED
    }

    public record EntryOutcome(String orcidId, Outcome outcome) {
    }

    private final OrcidMemberClient orcidMemberClient;
    private final MembershipFactory membershipFactory;
    private final RorClient rorClient;
    private final OrcidEntryRepository entryRepository;
    private final ResearcherRepository researcherRepository;
    private final InvitationRepository invitationRepository;
    private final TokenCipher tokenCipher;
    private final WorkQueue workQueue;
    private final TransactionTemplate transactionTemplate;
    private final OrcidUpdaterProperties properties;
    private final Clock clock;

    /** Embargoed RAiDs must not get here: the work processor holds them back (RAID-1005). */
    public List<EntryOutcome> sync(final OrcidRaid raid) {
        final var outcomes = new ArrayList<EntryOutcome>();
        // looked up once: every entry names it
        final var organisation = raid.contributors().isEmpty() ? null : rorClient.find(raid.ownerRorId()).orElse(null);
        for (final var contributor : raid.contributors()) {
            outcomes.add(new EntryOutcome(contributor.id(),
                    locked(contributor.id(), () -> write(raid, contributor, organisation))));
        }

        final var onRaid = raid.contributors().stream().map(Contributor::id).collect(Collectors.toSet());
        for (final var entry : entryRepository.findByRaidHandle(raid.handle())) {
            if (!onRaid.contains(entry.orcidId())) {
                outcomes.add(new EntryOutcome(entry.orcidId(), locked(entry.orcidId(), () -> remove(entry))));
            }
        }
        return outcomes;
    }

    private Outcome locked(final String orcidId, final Supplier<Outcome> work) {
        return transactionTemplate.execute(status -> {
            invitationRepository.lockOrcidId(orcidId);
            return work.get();
        });
    }

    private Outcome write(final OrcidRaid raid, final Contributor contributor, final OrcidOrganisation organisation) {
        final var orcidId = contributor.id();
        final var encryptedToken = researcherRepository.findAccessToken(orcidId);
        if (encryptedToken.isEmpty()) {
            return Outcome.NOT_AUTHENTICATED;
        }
        if (organisation == null) {
            // ORCID rejects a membership without an organisation, so there is no point calling it
            entryRepository.recordFailure(orcidId, raid.handle(), "The RAiD owner's organisation (" + raid.ownerRorId()
                    + ") couldn't be found in ROR, and ORCID requires one", clock.instant());
            return Outcome.FAILED;
        }
        final var token = decrypt(encryptedToken.get(), orcidId, raid.handle());
        if (token == null) {
            return Outcome.FAILED;
        }
        final var putCode = entryRepository.find(orcidId, raid.handle()).map(OrcidEntry::putCode).orElse(null);

        try {
            long written;
            if (putCode == null) {
                written = withRetries(() -> orcidMemberClient.create(orcidId, token,
                        membershipFactory.create(raid, contributor, null, organisation)));
            } else {
                try {
                    withRetries(() -> {
                        orcidMemberClient.update(orcidId, token, putCode, membershipFactory.create(raid, contributor, putCode, organisation));
                        return putCode;
                    });
                    written = putCode;
                } catch (final OrcidWriteException e) {
                    if (e.getKind() != Kind.NOT_FOUND) {
                        throw e;
                    }
                    // the entry was deleted at ORCID; the RAiD still names the researcher, so it is written again
                    log.info("RAiD {}: put code {} for {} no longer exists at ORCID, creating the entry again",
                            raid.handle(), putCode, orcidId);
                    written = withRetries(() -> orcidMemberClient.create(orcidId, token,
                            membershipFactory.create(raid, contributor, null, organisation)));
                }
            }
            entryRepository.recordSuccess(orcidId, raid.handle(), written, clock.instant());
            log.info("RAiD {}: wrote membership {} for {}", raid.handle(), written, orcidId);
            return Outcome.WRITTEN;
        } catch (final OrcidWriteException e) {
            return failed(orcidId, raid.handle(), "write", e);
        }
    }

    private Outcome remove(final OrcidEntry entry) {
        final var orcidId = entry.orcidId();
        if (entry.putCode() == null) {
            // never accepted by ORCID, so there is nothing to delete there
            entryRepository.delete(orcidId, entry.raidHandle());
            return Outcome.REMOVED;
        }
        final var encryptedToken = researcherRepository.findAccessToken(orcidId);
        if (encryptedToken.isEmpty()) {
            // kept, so the entry is removed if the researcher authenticates again
            entryRepository.recordFailure(orcidId, entry.raidHandle(),
                    "Contributor left the RAiD, but the entry can't be removed without the researcher's authorisation",
                    clock.instant());
            return Outcome.NOT_AUTHENTICATED;
        }
        final var token = decrypt(encryptedToken.get(), orcidId, entry.raidHandle());
        if (token == null) {
            return Outcome.FAILED;
        }
        try {
            withRetries(() -> {
                orcidMemberClient.delete(orcidId, token, entry.putCode());
                return entry.putCode();
            });
        } catch (final OrcidWriteException e) {
            if (e.getKind() != Kind.NOT_FOUND) {
                return failed(orcidId, entry.raidHandle(), "removal", e);
            }
            // already gone at ORCID
        }
        entryRepository.delete(orcidId, entry.raidHandle());
        log.info("RAiD {}: removed membership {} for {}, no longer a contributor", entry.raidHandle(), entry.putCode(), orcidId);
        return Outcome.REMOVED;
    }

    /** @return null, with the failure recorded, when the stored token can't be decrypted */
    private String decrypt(final byte[] encryptedToken, final String orcidId, final String raidHandle) {
        try {
            return tokenCipher.decrypt(encryptedToken, orcidId);
        } catch (final IllegalStateException e) {
            entryRepository.recordFailure(orcidId, raidHandle,
                    "The stored ORCID token can't be decrypted; was the token encryption key changed?", clock.instant());
            log.warn("RAiD {}: the stored ORCID token for {} can't be decrypted", raidHandle, orcidId);
            return null;
        }
    }

    private Outcome failed(final String orcidId, final String raidHandle, final String action, final OrcidWriteException e) {
        final var now = clock.instant();
        if (e.getKind() == Kind.TOKEN_REJECTED) {
            researcherRepository.markRevoked(orcidId, now);
            workQueue.requestStatus(orcidId);
            entryRepository.recordFailure(orcidId, raidHandle,
                    "ORCID rejected the researcher's token (access revoked): " + e.getMessage(), now);
            log.warn("RAiD {}: ORCID rejected the token for {}; marked revoked", raidHandle, orcidId);
            return Outcome.TOKEN_REJECTED;
        }
        entryRepository.recordFailure(orcidId, raidHandle, e.getMessage(), now);
        log.warn("RAiD {}: ORCID {} failed for {}: {}", raidHandle, action, orcidId, e.getMessage());
        return Outcome.FAILED;
    }

    /** Tries again after a 5xx, 429 or no response, waiting longer each time. */
    private long withRetries(final Supplier<Long> call) {
        final var maxAttempts = properties.getOrcidWrite().getMaxAttempts();
        var wait = properties.getOrcidWrite().getInitialBackoff();
        for (var attempt = 1; ; attempt++) {
            try {
                return call.get();
            } catch (final OrcidWriteException e) {
                if (e.getKind() != Kind.RETRYABLE || attempt >= maxAttempts) {
                    throw attempt > 1 ? new OrcidWriteException(e.getKind(),
                            e.getMessage() + " (after " + attempt + " attempts)") : e;
                }
            }
            try {
                Thread.sleep(wait.toMillis());
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new OrcidWriteException(Kind.RETRYABLE, "Interrupted while waiting to retry");
            }
            wait = wait.multipliedBy(2);
        }
    }
}
