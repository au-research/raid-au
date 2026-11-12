package au.org.raid.orcidupdater.deletion;

import au.org.raid.orcidupdater.crypto.EmailHasher;
import au.org.raid.orcidupdater.crypto.TokenCipher;
import au.org.raid.orcidupdater.invitation.InvitationRepository;
import au.org.raid.orcidupdater.orcid.OrcidEntryRepository;
import au.org.raid.orcidupdater.orcid.OrcidMemberClient;
import au.org.raid.orcidupdater.orcid.OrcidWriteException;
import au.org.raid.orcidupdater.researcher.ResearcherRepository;
import au.org.raid.orcidupdater.signin.OAuthStateRepository;
import au.org.raid.orcidupdater.signin.OrcidOAuthClient;
import au.org.raid.orcidupdater.work.WorkQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashSet;
import java.util.Optional;

/**
 * Deletes what the updater holds about a researcher when they ask (RAID-1006, Decision
 * 2026-10-23 Q5). In order: the entries it wrote to their ORCID record, then its access token
 * at ORCID, then everything stored here: tokens, consent, email fingerprint, put codes,
 * invitations naming the iD, and sign-ins in progress carrying the fingerprint.
 *
 * <p>All of it happens in one transaction holding the iD's lock, so a RAiD sync can't write a
 * new entry part-way through. If ORCID fails, or refuses to revoke the token, the transaction
 * is rolled back and nothing here is deleted, so the administrator can simply try again;
 * entries already removed at ORCID come back as 404 next time and count as removed.
 *
 * <p>When ORCID no longer accepts the token (the researcher revoked access there), the updater
 * can't remove the entries. It still deletes what it holds and reports the entries left, which
 * the researcher can remove on ORCID themselves.
 *
 * <p>The email is used only to find the fingerprint and is never logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResearcherDeletionService {
    private final ResearcherRepository researcherRepository;
    private final OrcidEntryRepository entryRepository;
    private final InvitationRepository invitationRepository;
    private final OAuthStateRepository oauthStateRepository;
    private final OrcidMemberClient orcidMemberClient;
    private final OrcidOAuthClient orcidOAuthClient;
    private final TokenCipher tokenCipher;
    private final EmailHasher emailHasher;
    private final WorkQueue workQueue;
    private final TransactionTemplate transactionTemplate;

    public ResearcherDeletionResponse delete(final ResearcherDeletionRequest request) {
        final var emailHash = isBlank(request.email()) ? null : emailHasher.hash(request.email());
        final var emailOwner = emailHash == null ? Optional.<String>empty() : researcherRepository.findOrcidIdByEmailHash(emailHash);
        final var givenId = isBlank(request.orcidId()) ? null : request.orcidId();
        if (givenId != null && emailOwner.isPresent() && !emailOwner.get().equals(givenId)) {
            throw new ResearcherMismatchException(givenId, emailOwner.get());
        }
        final var orcidId = givenId != null ? givenId : emailOwner.orElse(null);

        return transactionTemplate.execute(status -> {
            final var signInsDropped = emailHash == null ? 0 : oauthStateRepository.deleteByEmailHash(emailHash);
            if (orcidId == null) {
                if (signInsDropped > 0) {
                    log.info("Researcher deletion: dropped {} sign-in(s) in progress for an email held nowhere else",
                            signInsDropped);
                    return new ResearcherDeletionResponse(true, 0, 0, false);
                }
                log.info("Researcher deletion: nothing held for the email given");
                return ResearcherDeletionResponse.NOTHING_HELD;
            }
            invitationRepository.lockOrcidId(orcidId);
            return deleteHeld(orcidId, signInsDropped > 0);
        });
    }

    private ResearcherDeletionResponse deleteHeld(final String orcidId, final boolean droppedSignIns) {
        final var entries = entryRepository.findByOrcidId(orcidId);
        var token = researcherRepository.findAccessToken(orcidId).map(encrypted -> decrypt(encrypted, orcidId)).orElse(null);

        var deleted = 0;
        var left = 0;
        for (final var entry : entries) {
            if (entry.putCode() == null) {
                // never accepted by ORCID, so there is nothing there to remove
                continue;
            }
            if (token == null) {
                left++;
                continue;
            }
            try {
                orcidMemberClient.delete(orcidId, token, entry.putCode());
                deleted++;
            } catch (final OrcidWriteException e) {
                switch (e.getKind()) {
                    // already gone at ORCID, e.g. removed by the researcher or by an earlier attempt
                    case NOT_FOUND -> deleted++;
                    case TOKEN_REJECTED -> {
                        token = null;
                        left++;
                    }
                    case REJECTED -> {
                        log.warn("Researcher deletion: ORCID refused to remove membership {} for {}: {}",
                                entry.putCode(), orcidId, e.getMessage());
                        left++;
                    }
                    case RETRYABLE -> throw new OrcidUnavailableException(
                            "Removing membership %d for %s: %s".formatted(entry.putCode(), orcidId, e.getMessage()));
                }
            }
        }

        if (token != null) {
            try {
                orcidOAuthClient.revoke(token);
            } catch (final OrcidWriteException e) {
                throw new OrcidUnavailableException("Revoking the token for %s: %s".formatted(orcidId, e.getMessage()));
            }
        }

        entryRepository.deleteByOrcidId(orcidId);
        final var invitedIds = invitationRepository.deleteForOrcidId(orcidId);
        final var hadResearcher = researcherRepository.delete(orcidId);

        final var found = hadResearcher || !entries.isEmpty() || !invitedIds.isEmpty() || droppedSignIns;
        if (!found) {
            log.info("Researcher deletion: nothing held for {}", orcidId);
            return ResearcherDeletionResponse.NOTHING_HELD;
        }

        // the RAiD API shows the researcher as unauthenticated from now on, under the iD itself
        // and any iD a minter typed when inviting them
        final var statusIds = new LinkedHashSet<String>();
        statusIds.add(orcidId);
        statusIds.addAll(invitedIds);
        statusIds.forEach(workQueue::requestStatus);

        final var revoked = token != null;
        log.info("Researcher deletion: deleted the data held for {}; {} ORCID entries removed, {} left, token revoked: {}",
                orcidId, deleted, left, revoked);
        return new ResearcherDeletionResponse(true, deleted, left, revoked);
    }

    /** @return null when the stored token can't be decrypted, e.g. the encryption key changed */
    private String decrypt(final byte[] encrypted, final String orcidId) {
        try {
            return tokenCipher.decrypt(encrypted, orcidId);
        } catch (final IllegalStateException e) {
            log.warn("Researcher deletion: the stored ORCID token for {} can't be decrypted", orcidId);
            return null;
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
