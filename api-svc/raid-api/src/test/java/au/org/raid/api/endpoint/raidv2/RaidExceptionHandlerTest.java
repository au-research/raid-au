package au.org.raid.api.endpoint.raidv2;

import au.org.raid.idl.raidv2.model.ValidationFailureResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class RaidExceptionHandlerTest {

    private final RaidExceptionHandler handler = new RaidExceptionHandler();

    @Test
    @DisplayName("NotNull field error maps to notSet errorType with field's default message")
    void notNullFieldError_mapsToNotSet() {
        final var bindingResult = new BeanPropertyBindingResult(new Object(), "raidCreateRequest");
        bindingResult.addError(new FieldError("raidCreateRequest", "access", null, false,
                new String[]{"NotNull"}, null, "must not be null"));

        final var ex = new MethodArgumentNotValidException(null, bindingResult);
        final var response = handler.handleMethodArgumentNotValid(
                ex, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getHeaders().getContentType(), is(MediaType.APPLICATION_JSON));

        final var body = (ValidationFailureResponse) response.getBody();
        assertThat(body, notNullValue());
        assertThat(body.getType(), is("https://raid.org.au/errors#ValidationException"));
        assertThat(body.getTitle(), is("There were validation failures."));
        assertThat(body.getStatus(), is(400));
        assertThat(body.getInstance(), is("https://raid.org.au"));
        assertThat(body.getDetail(), is("Request had 1 validation failure(s). See failures for more details..."));

        final var failures = body.getFailures();
        assertThat(failures, hasSize(1));
        assertThat(failures.get(0).getFieldId(), is("access"));
        assertThat(failures.get(0).getErrorType(), is("notSet"));
        assertThat(failures.get(0).getMessage(), is("field must be set"));
    }

    @Test
    @DisplayName("NotBlank field error maps to notSet errorType")
    void notBlankFieldError_mapsToNotSet() {
        final var bindingResult = new BeanPropertyBindingResult(new Object(), "raidCreateRequest");
        bindingResult.addError(new FieldError("raidCreateRequest", "title[0].text", null, false,
                new String[]{"NotBlank"}, null, "must not be blank"));

        final var ex = new MethodArgumentNotValidException(null, bindingResult);
        final var response = handler.handleMethodArgumentNotValid(
                ex, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        final var body = (ValidationFailureResponse) response.getBody();
        assertThat(body, notNullValue());
        final var failure = body.getFailures().get(0);
        assertThat(failure.getFieldId(), is("title[0].text"));
        assertThat(failure.getErrorType(), is("notSet"));
        assertThat(failure.getMessage(), is("field must be set"));
    }

    @Test
    @DisplayName("NotEmpty field error maps to notSet errorType")
    void notEmptyFieldError_mapsToNotSet() {
        final var bindingResult = new BeanPropertyBindingResult(new Object(), "raidCreateRequest");
        bindingResult.addError(new FieldError("raidCreateRequest", "titles", null, false,
                new String[]{"NotEmpty"}, null, "must not be empty"));

        final var ex = new MethodArgumentNotValidException(null, bindingResult);
        final var response = handler.handleMethodArgumentNotValid(
                ex, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        final var body = (ValidationFailureResponse) response.getBody();
        assertThat(body, notNullValue());
        final var failure = body.getFailures().get(0);
        assertThat(failure.getFieldId(), is("titles"));
        assertThat(failure.getErrorType(), is("notSet"));
    }

    @Test
    @DisplayName("Pattern field error maps to invalidValue errorType")
    void patternFieldError_mapsToInvalidValue() {
        final var bindingResult = new BeanPropertyBindingResult(new Object(), "raidCreateRequest");
        bindingResult.addError(new FieldError("raidCreateRequest", "identifier.id", "bad-value", false,
                new String[]{"Pattern"}, null, "must match \"^https://raid.org/.*\""));

        final var ex = new MethodArgumentNotValidException(null, bindingResult);
        final var response = handler.handleMethodArgumentNotValid(
                ex, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        final var body = (ValidationFailureResponse) response.getBody();
        assertThat(body, notNullValue());
        final var failure = body.getFailures().get(0);
        assertThat(failure.getFieldId(), is("identifier.id"));
        assertThat(failure.getErrorType(), is("invalidValue"));
        assertThat(failure.getMessage(), is("field has an invalid value"));
    }

    @Test
    @DisplayName("Size field error maps to invalidValue errorType")
    void sizeFieldError_mapsToInvalidValue() {
        final var bindingResult = new BeanPropertyBindingResult(new Object(), "raidCreateRequest");
        bindingResult.addError(new FieldError("raidCreateRequest", "description[0].text", "x", false,
                new String[]{"Size"}, null, "size must be between 1 and 2000"));

        final var ex = new MethodArgumentNotValidException(null, bindingResult);
        final var response = handler.handleMethodArgumentNotValid(
                ex, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        final var body = (ValidationFailureResponse) response.getBody();
        assertThat(body, notNullValue());
        final var failure = body.getFailures().get(0);
        assertThat(failure.getErrorType(), is("invalidValue"));
    }

    @Test
    @DisplayName("Pattern field error with blank rejected value maps to notSet errorType")
    void patternFieldErrorWithBlankValue_mapsToNotSet() {
        final var bindingResult = new BeanPropertyBindingResult(new Object(), "raidCreateRequest");
        bindingResult.addError(new FieldError("raidCreateRequest", "access.statement.language.id", "", false,
                new String[]{"Pattern"}, null, "must match \"^\\s*\\S.*$\""));

        final var ex = new MethodArgumentNotValidException(null, bindingResult);
        final var response = handler.handleMethodArgumentNotValid(
                ex, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        final var body = (ValidationFailureResponse) response.getBody();
        assertThat(body, notNullValue());
        final var failure = body.getFailures().get(0);
        assertThat(failure.getFieldId(), is("access.statement.language.id"));
        assertThat(failure.getErrorType(), is("notSet"));
        assertThat(failure.getMessage(), is("field must be set"));
    }

    @Test
    @DisplayName("Multiple field errors are all included in failures list")
    void multipleErrors_allIncluded() {
        final var bindingResult = new BeanPropertyBindingResult(new Object(), "raidCreateRequest");
        bindingResult.addError(new FieldError("raidCreateRequest", "access", null, false,
                new String[]{"NotNull"}, null, "must not be null"));
        bindingResult.addError(new FieldError("raidCreateRequest", "identifier.id", "bad", false,
                new String[]{"Pattern"}, null, "must match pattern"));

        final var ex = new MethodArgumentNotValidException(null, bindingResult);
        final var response = handler.handleMethodArgumentNotValid(
                ex, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        final var body = (ValidationFailureResponse) response.getBody();
        assertThat(body, notNullValue());
        assertThat(body.getDetail(), is("Request had 2 validation failure(s). See failures for more details..."));
        assertThat(body.getFailures(), hasSize(2));
    }

    @Test
    @DisplayName("Null defaultMessage falls back to sensible default for notSet")
    void nullDefaultMessage_fallsBackForNotSet() {
        final var bindingResult = new BeanPropertyBindingResult(new Object(), "raidCreateRequest");
        bindingResult.addError(new FieldError("raidCreateRequest", "access", null, false,
                new String[]{"NotNull"}, null, null));

        final var ex = new MethodArgumentNotValidException(null, bindingResult);
        final var response = handler.handleMethodArgumentNotValid(
                ex, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        final var body = (ValidationFailureResponse) response.getBody();
        assertThat(body, notNullValue());
        assertThat(body.getFailures().get(0).getMessage(), is("field must be set"));
    }

    @Test
    @DisplayName("Null defaultMessage falls back to sensible default for invalidValue")
    void nullDefaultMessage_fallsBackForInvalidValue() {
        final var bindingResult = new BeanPropertyBindingResult(new Object(), "raidCreateRequest");
        bindingResult.addError(new FieldError("raidCreateRequest", "identifier.id", "bad", false,
                new String[]{"Pattern"}, null, null));

        final var ex = new MethodArgumentNotValidException(null, bindingResult);
        final var response = handler.handleMethodArgumentNotValid(
                ex, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        final var body = (ValidationFailureResponse) response.getBody();
        assertThat(body, notNullValue());
        assertThat(body.getFailures().get(0).getMessage(), is("field has an invalid value"));
    }

    @Test
    @DisplayName("Non-field ObjectError uses objectName as fieldId and maps unknown code to invalidValue")
    void objectError_usesObjectNameAsFieldId() {
        final var bindingResult = new BeanPropertyBindingResult(new Object(), "raidCreateRequest");
        bindingResult.addError(new ObjectError("raidCreateRequest",
                new String[]{"CustomObjectConstraint"}, null, "object-level constraint violated"));

        final var ex = new MethodArgumentNotValidException(null, bindingResult);
        final var response = handler.handleMethodArgumentNotValid(
                ex, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        final var body = (ValidationFailureResponse) response.getBody();
        assertThat(body, notNullValue());
        final var failure = body.getFailures().get(0);
        assertThat(failure.getFieldId(), is("raidCreateRequest"));
        assertThat(failure.getErrorType(), is("invalidValue"));
        assertThat(failure.getMessage(), is("field has an invalid value"));
    }
}
