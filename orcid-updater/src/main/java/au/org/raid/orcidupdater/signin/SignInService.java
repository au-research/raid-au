package au.org.raid.orcidupdater.signin;

import au.org.raid.orcidupdater.crypto.InvitationLinkToken;
import au.org.raid.orcidupdater.crypto.LinkCipher;
import au.org.raid.orcidupdater.crypto.TokenCipher;
import au.org.raid.orcidupdater.invitation.Invitation;
import au.org.raid.orcidupdater.invitation.InvitationRepository;
import au.org.raid.orcidupdater.researcher.ResearcherRepository;
import au.org.raid.orcidupdater.signin.InvalidInvitationLinkException.Reason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * The researcher's side of RAID-975: opening the invitation link, choosing "Remember me",
 * signing in to ORCID and coming back. Invitation tokens, OAuth state, codes and ORCID tokens
 * are never logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignInService {
    /** How long a researcher has to finish signing in at ORCID after clicking the button. */
    static final Duration SIGN_IN_TIMEOUT = Duration.ofMinutes(30);
    private static final int STATE_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern OAUTH_ERROR_CODE = Pattern.compile("^[a-z_]{1,64}$");

    private final InvitationRepository invitationRepository;
    private final OAuthStateRepository oauthStateRepository;
    private final ResearcherRepository researcherRepository;
    private final OrcidOAuthClient orcidOAuthClient;
    private final LinkCipher linkCipher;
    private final TokenCipher tokenCipher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /** @throws InvalidInvitationLinkException the link is malformed, unknown, used or expired */
    public Invitation openInvitation(final String linkToken) {
        return pending(parse(linkToken));
    }

    /**
     * Records the sign-in attempt and returns ORCID's authorisation URL. With "Remember me", the
     * email fingerprint is opened from the link and held with the attempt until ORCID confirms
     * who the researcher is.
     *
     * @throws InvalidInvitationLinkException the link is malformed, unknown, used or expired
     */
    public URI startSignIn(final String linkToken, final boolean rememberMe) {
        final var token = parse(linkToken);
        final var invitation = pending(token);

        byte[] emailHash = null;
        if (rememberMe) {
            // a link whose fingerprint doesn't open was altered, so it is treated as unknown
            emailHash = linkCipher.decrypt(token.sealedEmailHash(), token.random())
                    .orElseThrow(() -> new InvalidInvitationLinkException(Reason.UNKNOWN));
        }

        final var state = new byte[STATE_BYTES];
        SECURE_RANDOM.nextBytes(state);
        final var stateValue = Base64.getUrlEncoder().withoutPadding().encodeToString(state);
        final var now = clock.instant();
        oauthStateRepository.insert(sha256(stateValue), invitation.id(), emailHash, now, now.plus(SIGN_IN_TIMEOUT));

        log.info("Invitation {} for {} sent to ORCID sign-in, remember me {}",
                invitation.id(), invitation.orcidId(), rememberMe);
        return orcidOAuthClient.authorisationUri(stateValue);
    }

    /**
     * Finishes sign-in with the parameters ORCID added when it sent the researcher back to the app.
     *
     * @throws InvalidOAuthStateException     the state matches no sign-in in progress, or there is no code
     * @throws InvalidInvitationLinkException the invitation was used or expired while the researcher was at ORCID
     * @throws OrcidSignInFailedException     ORCID would not exchange the code
     */
    public SignInResult completeSignIn(final String state, final String code, final String error) {
        if (state == null || state.isBlank()) {
            throw new InvalidOAuthStateException("OAuth callback without state");
        }
        // consumed whatever happens next, so the same callback can't be used twice
        final var attempt = oauthStateRepository.consume(sha256(state))
                .orElseThrow(() -> new InvalidOAuthStateException("OAuth callback state matches no sign-in"));
        final var now = clock.instant();
        if (!attempt.expiresAt().isAfter(now)) {
            throw new InvalidOAuthStateException("OAuth callback state expired");
        }
        final var invitation = invitationRepository.findById(attempt.invitationId())
                .orElseThrow(() -> new InvalidOAuthStateException("OAuth callback state has no invitation"));

        if (error != null) {
            // the researcher declined, or ORCID refused; nothing is stored and the link still works
            log.info("Invitation {} for {}: ORCID sign-in ended with {}", invitation.id(), invitation.orcidId(),
                    OAUTH_ERROR_CODE.matcher(error).matches() ? error : "an unrecognised error");
            return SignInResult.declined(invitation);
        }
        if (code == null || code.isBlank()) {
            throw new InvalidOAuthStateException("OAuth callback without code or error");
        }
        if (invitation.isUsed()) {
            throw new InvalidInvitationLinkException(Reason.USED);
        }
        if (invitation.isExpiredAt(now)) {
            throw new InvalidInvitationLinkException(Reason.EXPIRED);
        }

        final var signIn = orcidOAuthClient.exchange(code);
        final var orcidId = signIn.orcidId();
        final var remembered = attempt.emailHash() != null;

        transactionTemplate.executeWithoutResult(status -> {
            invitationRepository.lockOrcidId(orcidId);
            if (!invitationRepository.markUsed(invitation.id(), orcidId, now)) {
                // another callback for the same link got here first; this token is not kept
                throw new InvalidInvitationLinkException(Reason.USED);
            }
            researcherRepository.saveAuthentication(orcidId, tokenCipher.encrypt(signIn.accessToken(), orcidId),
                    signIn.refreshToken() == null ? null : tokenCipher.encrypt(signIn.refreshToken(), orcidId),
                    signIn.scope(), now);
            if (remembered) {
                researcherRepository.recordConsent(orcidId, attempt.emailHash(), now);
            }
        });

        // IMPROVE: RAID-1005 pushes AUTHENTICATED to the RAiD API and RAID-1004 writes the RAiD to ORCID here
        log.info("Invitation {}: authenticated as {} (typed {}), remember me {}",
                invitation.id(), orcidId, invitation.orcidId(), remembered);
        return SignInResult.authenticated(invitation, orcidId, remembered);
    }

    private static InvitationLinkToken parse(final String linkToken) {
        return InvitationLinkToken.parse(linkToken).orElseThrow(() -> new InvalidInvitationLinkException(Reason.UNKNOWN));
    }

    private Invitation pending(final InvitationLinkToken token) {
        final var invitation = invitationRepository.findByTokenHash(token.lookupHash())
                .orElseThrow(() -> new InvalidInvitationLinkException(Reason.UNKNOWN));
        if (invitation.isUsed()) {
            throw new InvalidInvitationLinkException(Reason.USED);
        }
        if (invitation.isExpiredAt(clock.instant())) {
            throw new InvalidInvitationLinkException(Reason.EXPIRED);
        }
        return invitation;
    }

    private static byte[] sha256(final String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public enum Outcome { AUTHENTICATED, DECLINED }

    /**
     * Contract schema AuthenticationResult: what the app shows after ORCID sends the researcher
     * back. {@code orcidId} is the iD the researcher signed in with and is null when declined;
     * it can differ from {@code typedOrcidId}, the iD the minter entered.
     */
    public record SignInResult(Outcome outcome, String orcidId, String typedOrcidId, String raidTitle,
                               String servicePointName, boolean remembered, Instant invitationExpiresAt) {
        static SignInResult declined(final Invitation invitation) {
            return new SignInResult(Outcome.DECLINED, null, invitation.orcidId(), invitation.raidTitle(),
                    invitation.servicePointName(), false, invitation.expiresAt());
        }

        static SignInResult authenticated(final Invitation invitation, final String orcidId, final boolean remembered) {
            return new SignInResult(Outcome.AUTHENTICATED, orcidId, invitation.orcidId(), invitation.raidTitle(),
                    invitation.servicePointName(), remembered, invitation.expiresAt());
        }
    }
}
