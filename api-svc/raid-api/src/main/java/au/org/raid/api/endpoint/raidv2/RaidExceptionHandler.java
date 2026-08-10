package au.org.raid.api.endpoint.raidv2;

import au.org.raid.api.exception.*;
import au.org.raid.idl.raidv2.model.ClosedRaid;
import au.org.raid.idl.raidv2.model.FailureResponse;
import au.org.raid.idl.raidv2.model.ResolverUnavailableResponse;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import au.org.raid.idl.raidv2.model.ValidationFailureResponse;
import lombok.extern.slf4j.Slf4j;
import org.jooq.exception.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Set;

@Slf4j
@ControllerAdvice
public class RaidExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ClosedRaidException.class)
    public ResponseEntity<ClosedRaid> handleClosedRaidException(final ClosedRaidException e) {
        final var raid = e.getRaid();

        final var body = new ClosedRaid()
                .id(raid.getIdentifier().getId())
                .access(raid.getAccess());

        return ResponseEntity.status(403).body(body);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationFailureResponse> handleValidationException(final Exception e) {
        final var exception = (ValidationException) e;

        final var body = new ValidationFailureResponse()
                .type(exception.getType())
                .title(exception.getTitle())
                .status(exception.getStatus())
                .detail(exception.getDetail())
                .instance(exception.getInstance())
                .failures(exception.getFailures());

        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /*
    RAID-809: an external identifier resolver (ROR, ORCID, ISNI, DOI, Handle, RRID, GeoNames,
    OpenStreetMap) was unreachable or errored while validating a mint/update/patch request.
    This is more specific than both the generic RaidApiException handler and the RAID-803
    RestClientException -> 502 handler (ResolverUnavailableException is neither a bare
    RestClientException nor handled generically - it's converted from one at the validator
    layer), and is deliberately a 503 (retryable), not a 400 (the caller's input wasn't the
    problem) or a 502 (the failure is scoped and enumerated, not an opaque upstream failure).
     */
    @ExceptionHandler(ResolverUnavailableException.class)
    public ResponseEntity<ResolverUnavailableResponse> handleResolverUnavailable(final ResolverUnavailableException e) {
        log.warn("External resolver(s) unavailable: {}", e.getMessage());

        final var body = new ResolverUnavailableResponse()
                .type(e.getType())
                .title(e.getTitle())
                .status(e.getStatus())
                .detail(e.getDetail())
                .instance(e.getInstance())
                .unavailableResolvers(e.getUnavailableResolvers());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "30")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(CrossAccountAccessException.class)
    public ResponseEntity<FailureResponse> handleCrossAccountException(final Exception e) {
        final var exception = (CrossAccountAccessException) e;

        final var body = new FailureResponse()
                .type(exception.getType())
                .title(exception.getTitle())
                .status(exception.getStatus())
                .detail(exception.getDetail())
                .instance(exception.getInstance());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<FailureResponse> handleRaidApiException(final Exception e) {
        final var exception = (ResourceNotFoundException) e;

        final var body = new FailureResponse()
                .type(exception.getType())
                .title(exception.getTitle())
                .status(exception.getStatus())
                .detail(exception.getDetail())
                .instance(exception.getInstance());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(InvalidJsonException.class)
    public ResponseEntity<FailureResponse> handleInvalidJsonException(final Exception e) {
        final var exception = (InvalidJsonException) e;

        final var body = new FailureResponse()
                .type(exception.getType())
                .title(exception.getTitle())
                .status(exception.getStatus())
                .detail(exception.getDetail())
                .instance(exception.getInstance());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(RaidApiException.class)
    public ResponseEntity<FailureResponse> handle(final RaidApiException e) {
        final var body = new FailureResponse()
                .type(e.getType())
                .title(e.getTitle())
                .status(e.getStatus())
                .detail(e.getDetail())
                .instance(e.getInstance());

        return ResponseEntity
                .status(HttpStatusCode.valueOf(e.getStatus()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /*
    This is app-wide, not mint-specific: as a @ControllerAdvice handler it covers ANY controller's
    uncaught RestClientException, from any outbound-call failure - ResourceAccessException
    (connect/read timeout, DNS failure, connection refused), HttpServerErrorException (5xx), and
    HttpClientErrorException (4xx, since it extends RestClientException). Mint (via ROR/DataCite,
    RaidService.mintHandle's 422-retry aside - those are caught and retried there and never reach
    this handler) is the primary case, but this also covers e.g. DataciteRepositoryClient during
    service-point provisioning or RaidUpgradeService. Falling through to defaultExceptionHandler
    previously produced an opaque, empty-body 500; this returns a structured 502 so the caller
    gets a JSON body identifying the failure as an upstream dependency problem. See RAID-803.
     */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<FailureResponse> handleRestClientException(final RestClientException e) {
        log.error("Upstream service call failed", e);

        final var body = new FailureResponse()
                .type("https://raid.org.au/errors#UpstreamServiceException")
                .title("Upstream service unavailable")
                .status(HttpStatus.BAD_GATEWAY.value())
                .detail("An upstream dependency did not respond. Please try again later.")
                .instance("https://raid.org.au");

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<FailureResponse> dataAccessExceptionHandler(final Exception e) {
        log.error("Database access error", e);

        final var body = new FailureResponse()
                .type("https://raid.org.au/errors#InternalServerError")
                .title("Internal server error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("A database error occurred. Please try again later.")
                .instance("https://raid.org.au");

        return ResponseEntity
                .internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> defaultExceptionHandler(final Exception e, final WebRequest request) {
        try {
            final ResponseEntity<Object> response = super.handleException(e, request);
            if (response != null) {
                return response;
            }
        } catch (Exception handlerEx) {
            log.error("Exception thrown in exception handler", handlerEx);
        }
        log.error("Unhandled exception", e);

        // Generic detail only - the exception message/stacktrace is logged above, not exposed
        // to the caller, so this can't leak internals.
        final var body = new FailureResponse()
                .type("https://raid.org.au/errors#InternalServerError")
                .title("Internal server error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("An unexpected error occurred. Please try again later.")
                .instance("https://raid.org.au");

        return ResponseEntity
                .internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body((Object) body);
    }

    /*
    Added this to fix LegacyRaidV1MintTest. Can probably be deleted once new error handling is supported by the app.
     */
//    @ExceptionHandler(ApiSafeException.class)
//    public ResponseEntity<RedactingExceptionResolver.ErrorJson> handleApiSafeException(Exception e) {
//
//        final var errorJson = new RedactingExceptionResolver.ErrorJson();
//        errorJson.detail = ((ApiSafeException) e).getDetail();
//        errorJson.status = ((ApiSafeException) e).getHttpStatus();
//        errorJson.message = e.getMessage();
//
//        return ResponseEntity
//                .badRequest()
//                .body(errorJson);
//    }

    @ExceptionHandler(InvalidAccessException.class)
    public ResponseEntity<FailureResponse> handleInvalidAccessException(final Exception e) {
        final var exception = (InvalidAccessException) e;
        log.warn(exception.getTitle(), e);

        final var body = new FailureResponse()
                .type(exception.getType())
                .title(exception.getTitle())
                .status(exception.getStatus())
                .detail(exception.getDetail())
                .instance(exception.getInstance());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(InvalidVersionException.class)
    public ResponseEntity<FailureResponse> handleInvalidVersionException(final Exception e) {
        final var exception = (InvalidVersionException) e;
        log.warn(exception.getTitle(), e);

        final var body = new FailureResponse()
                .type(exception.getType())
                .title(exception.getTitle())
                .status(exception.getStatus())
                .detail(exception.getDetail())
                .instance(exception.getInstance());

        return ResponseEntity
                .status(exception.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        final var failures = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return toValidationFailure(fieldError);
                    }
                    return toValidationFailure(error);
                })
                .toList();

        final var failureCount = failures.size();
        final var body = new ValidationFailureResponse()
                .type("https://raid.org.au/errors#ValidationException")
                .title("There were validation failures.")
                .status(400)
                .detail("Request had %d validation failure(s). See failures for more details...".formatted(failureCount))
                .instance("https://raid.org.au")
                .failures(failures);

        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private static final Set<String> NOT_SET_CODES = Set.of("NotNull", "NotBlank", "NotEmpty");

    private ValidationFailure toValidationFailure(FieldError fieldError) {
        var rejectedValue = fieldError.getRejectedValue();
        var isBlankValue = rejectedValue == null || (rejectedValue instanceof String s && s.isBlank());
        var errorType = (NOT_SET_CODES.contains(fieldError.getCode()) || isBlankValue) ? "notSet" : "invalidValue";
        var message = errorType.equals("notSet") ? "field must be set" : "field has an invalid value";
        return new ValidationFailure(fieldError.getField(), errorType, message);
    }

    private ValidationFailure toValidationFailure(ObjectError error) {
        var errorType = NOT_SET_CODES.contains(error.getCode()) ? "notSet" : "invalidValue";
        var message = errorType.equals("notSet") ? "field must be set" : "field has an invalid value";
        return new ValidationFailure(error.getObjectName(), errorType, message);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
    /* I wanted to log the input message that caused the error here too, but
    I couldn't figure out how to actually print the content of
    ex.httpInputMessage.  So it's logged from the RequestLoggingFilter.
    */
        log.warn(ex.getMessage());

        final var typeFormat = "https://raid.org.au/errors#%s";

        final var body = new FailureResponse()
                .type(String.format(typeFormat, "InvalidJsonException"))
                .title("Invalid JSON")
                .status(400)
                .detail(ex.getMessage())
                .instance("https://raid.org.au");

        return ResponseEntity
                .status(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}