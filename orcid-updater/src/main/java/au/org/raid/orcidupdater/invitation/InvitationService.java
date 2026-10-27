package au.org.raid.orcidupdater.invitation;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.crypto.EmailHasher;
import au.org.raid.orcidupdater.crypto.InvitationLinkToken;
import au.org.raid.orcidupdater.crypto.LinkCipher;
import au.org.raid.orcidupdater.raidapi.RaidApiClient;
import au.org.raid.orcidupdater.researcher.ResearcherRepository;
import au.org.raid.orcidupdater.status.ContributorStatus;
import au.org.raid.orcidupdater.work.WorkQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.UUID;

/**
 * Registers an invitation for a contributor (RAID-975, contract operation registerInvitation).
 * The email address is used to send the invitation and to compute its fingerprint, and is never
 * stored or logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {
    private final RaidApiClient raidApiClient;
    private final InvitationRepository invitationRepository;
    private final ResearcherRepository researcherRepository;
    private final InvitationEmailSender emailSender;
    private final EmailHasher emailHasher;
    private final LinkCipher linkCipher;
    private final OrcidUpdaterProperties properties;
    private final WorkQueue workQueue;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public InvitationResponse register(final InvitationRequest request, final String minterToken) {
        // outside the transaction: these are remote calls, and they hold no lock
        final var raid = raidApiClient.findEditableRaid(request.handle(), minterToken);
        if (!raid.contributorIds().contains(request.contributorId())) {
            throw new ContributorNotOnRaidException(request.contributorId(), request.handle());
        }

        final var emailHash = emailHasher.hash(request.email());

        // the email is sent inside the transaction, so a failed send rolls the invitation back
        return transactionTemplate.execute(status -> {
            invitationRepository.lockOrcidId(request.contributorId());

            final var remembered = researcherRepository.findOrcidIdByConsentedEmailHash(emailHash);
            if (remembered.isPresent()) {
                log.info("Invitation for {} on {} matched a researcher who chose Remember me; no email sent",
                        request.contributorId(), request.handle());
                // the researcher is already authenticated, so the RAiD can be written straight away
                workQueue.requestRaidSync(request.handle());
                return new InvitationResponse(remembered.get(), ContributorStatus.AUTHENTICATED, false, null);
            }

            final var now = clock.instant();
            final var pending = invitationRepository.findPendingExpiry(request.contributorId(), now);
            if (pending.isPresent()) {
                log.info("Invitation for {} on {} already pending until {}; no email sent",
                        request.contributorId(), request.handle(), pending.get());
                return new InvitationResponse(request.contributorId(), ContributorStatus.AWAITING_AUTHENTICATION,
                        false, pending.get());
            }

            final var token = InvitationLinkToken.issue(emailHash, linkCipher);
            final var id = UUID.randomUUID();
            final var expiresAt = now.plus(properties.getInvitation().getExpiry());
            invitationRepository.insert(id, token.lookupHash(), request.contributorId(), raid, now, expiresAt);
            workQueue.requestStatus(request.contributorId());

            final var link = properties.appUrl("/invitations/" + token.encode());
            emailSender.send(request.email(), raid, link, expiresAt);

            log.info("Invitation {} sent for {} on {}, expires {}",
                    id, request.contributorId(), request.handle(), expiresAt);
            return new InvitationResponse(request.contributorId(), ContributorStatus.AWAITING_AUTHENTICATION,
                    true, expiresAt);
        });
    }
}
