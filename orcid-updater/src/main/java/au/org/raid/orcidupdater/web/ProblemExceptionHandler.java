package au.org.raid.orcidupdater.web;

import au.org.raid.orcidupdater.invitation.ContributorNotOnRaidException;
import au.org.raid.orcidupdater.invitation.EmailDeliveryException;
import au.org.raid.orcidupdater.raidapi.MinterNotPermittedException;
import au.org.raid.orcidupdater.raidapi.RaidApiUnavailableException;
import au.org.raid.orcidupdater.raidapi.RaidNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * RFC 9457 problem details for the updater's API. Details are built from field names and fixed
 * text only, never from submitted values, so an email address can't be echoed back or logged.
 */
@Slf4j
@RestControllerAdvice
public class ProblemExceptionHandler {
    static final String CODE = "code";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalid(final MethodArgumentNotValidException e) {
        final var detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadable(final HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request body is not valid JSON for this operation.");
    }

    @ExceptionHandler(MinterNotPermittedException.class)
    public ProblemDetail notPermitted(final MinterNotPermittedException e) {
        return problem(HttpStatus.FORBIDDEN, "NOT_PERMITTED", "You can't edit this RAiD.");
    }

    @ExceptionHandler(RaidNotFoundException.class)
    public ProblemDetail raidNotFound(final RaidNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "RAID_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(ContributorNotOnRaidException.class)
    public ProblemDetail notAContributor(final ContributorNotOnRaidException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "NOT_A_CONTRIBUTOR", e.getMessage());
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ProblemDetail emailFailed(final EmailDeliveryException e) {
        log.warn(e.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "EMAIL_DELIVERY_FAILED",
                "The invitation email could not be sent. No invitation was recorded.");
    }

    @ExceptionHandler(RaidApiUnavailableException.class)
    public ProblemDetail raidApiUnavailable(final RaidApiUnavailableException e) {
        log.warn(e.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "RAID_API_UNAVAILABLE", "The RAiD API could not be reached.");
    }

    private static ProblemDetail problem(final HttpStatus status, final String code, final String detail) {
        final var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(CODE, code);
        return problem;
    }
}
