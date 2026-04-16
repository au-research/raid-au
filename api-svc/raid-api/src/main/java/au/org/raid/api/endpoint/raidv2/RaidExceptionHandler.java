package au.org.raid.api.endpoint.raidv2;

import au.org.raid.api.exception.*;
import au.org.raid.idl.raidv2.model.ClosedRaid;
import au.org.raid.idl.raidv2.model.FailureResponse;
import au.org.raid.idl.raidv2.model.ValidationFailureResponse;
import lombok.extern.slf4j.Slf4j;
import org.jooq.exception.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class RaidExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ClosedRaidException.class)
    public ResponseEntity<ClosedRaid> handleClosedRaidException(final ClosedRaidException e) {
        final var raid = e.getRaid();

        final var body = new ClosedRaid()
                .identifier(raid.getIdentifier())
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

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Void> dataAccessExceptionHandler(final Exception e) {
        return ResponseEntity
                .internalServerError()
                .build();
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

        return ResponseEntity
                .internalServerError()
                .build();
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