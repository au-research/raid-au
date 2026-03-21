package au.org.raid.api.service.raid.id;

import au.org.raid.api.endpoint.message.ValidationMessage;
import au.org.raid.api.exception.ValidationFailureException;
import au.org.raid.api.util.Nullable;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static au.org.raid.api.util.ExceptionUtil.illegalArgException;
import static au.org.raid.api.util.RestUtil.normaliseWithoutTrailingSlash;
import static au.org.raid.api.util.StringUtil.isBlank;

/**
 * IMPROVE:
 * - match specific configured prefixes only (102.100.100, 10378.1, etc.)
 * - match only "proper" prefixes composed of digits and decimal separators
 * - optimise for garbage allocation rate, CPU used
 */
@Component
public class IdentifierParser {
    public static final char HANDLE_SEPARATOR_CHAR = '/';
    public static final String HANDLE_SEPARATOR =
            String.valueOf(HANDLE_SEPARATOR_CHAR);

    public static List<ValidationFailure> mapProblemsToValidationFailures(
            ParseProblems problems
    ) {
        return problems.getProblems().stream().map(
                ValidationMessage::invalidIdentifier
        ).toList();
    }

    /**
     * Expects an url that has been passed from Spring MVC, so:
     * - assume Spring already decoded the url if it was encoded
     * - assume that deep paths that LOOK like prefixes would have already been
     * directed to their mapping endpoint and not get passed, so we choose to assume
     * that extra stuff on the end of the url is in the nature of a "description"
     * (like StackOverflow question titles).
     */
    public ParseHandleResult parseHandle(@Nullable String urlPathInput) {
        if (isBlank(urlPathInput)) {
            return new ParseProblems("IdentifierHandle may not be blank");
        }

        String handle = urlPathInput.trim();
        //? handle = handle.toLowerCase();

        if (handle.charAt(0) == HANDLE_SEPARATOR_CHAR) {
            handle = handle.substring(1);
        }

        String[] split = handle.split(HANDLE_SEPARATOR);
        if (split.length < 2) {
            return new ParseProblems(
                    "IdentifierHandle not enough parts: " + urlPathInput);
        }

        if (isBlank(split[0])) {
            return new ParseProblems(
                    "IdentifierHandle prefix may not be blank: " + urlPathInput);
        }
        if (isBlank(split[1])) {
            return new ParseProblems(
                    "IdentifierHandle suffix may not be blank: " + urlPathInput);
        }

        if (split.length == 2) {
            return new IdentifierHandle(split[0], split[1], Optional.empty());
        }

        return new IdentifierHandle(
                split[0],
                split[1],
                Optional.of(String.join(
                        HANDLE_SEPARATOR,
                        Arrays.copyOfRange(split, 2, split.length)
                ))
        );
    }

    /**
     * @param urlInput is expected to be "full" url, of the form
     *                 `https://example.com/prefix/suffix/`, a "relative" url or url without
     *                 protocol will fail.
     */
    public ParseUrlResult parseUrl(String urlInput) {
        if (isBlank(urlInput)) {
            return new ParseProblems("identifier may not be blank");
        }

        String urlTemp = normaliseWithoutTrailingSlash(urlInput);
        //? handle = handle.toLowerCase();

        URL url = null;
        try {
            url = new URL(urlTemp);
        } catch (MalformedURLException e) {
            // so far when tested the exception shows the input used
            return new ParseProblems("Identifier is not an URL - " + e.getMessage());
        }

        var parseResult = parseHandle(url.getPath());

        if (isBlank(url.getHost())) {
            return new ParseProblems("host may not be blank: " + urlInput);
        }

        String urlPrefix = "%s://%s".formatted(
                url.getProtocol(), url.getAuthority());

        if (parseResult instanceof ParseProblems problems) {
            return problems;
        } else if (parseResult instanceof IdentifierHandle handle) {
            return new IdentifierUrl(urlPrefix, handle);
        } else {
            throw new RuntimeException("unknown sealed type: " + parseResult);
        }
    }

    public IdentifierUrl parseUrlWithException(String input)
            throws ValidationFailureException {
        var result = parseUrl(input);
        if (result instanceof ParseProblems problems) {
            throw new ValidationFailureException(
                    mapProblemsToValidationFailures(problems));
        } else if (result instanceof IdentifierUrl url) {
            return url;
        } else {
            throw illegalArgException("unknown sealed type: %s", result);
        }
    }

    public IdentifierUrl parseUrlWithRuntimeException(String input) {
        try {
            return parseUrlWithException(input);
        } catch (ValidationFailureException e) {
            throw new RuntimeException(e);
        }
    }

    public IdentifierHandle parseHandleWithException(String input)
            throws ValidationFailureException {
        var result = parseHandle(input);
        if (result instanceof ParseProblems problems) {
            throw new ValidationFailureException(
                    mapProblemsToValidationFailures(problems));
        } else if (result instanceof IdentifierHandle handle) {
            return handle;
        } else {
            throw illegalArgException("unknown sealed type: %s", result);
        }
    }

    public void parseUrl(
            String input,
            Consumer<IdentifierUrl> onResult,
            Consumer<ParseProblems> onError
    ) {
        var result = parseUrl(input);
        if (result instanceof ParseProblems problems) {
            onError.accept(problems);
        } else if (result instanceof IdentifierUrl id) {
            onResult.accept(id);
        } else {
            throw illegalArgException("unknown sealed type: " + result);
        }
    }

    public void parseHandle(
            String input,
            Consumer<IdentifierHandle> onResult,
            Consumer<ParseProblems> onError
    ) {
        var result = parseHandle(input);
        if (result instanceof ParseProblems problems) {
            onError.accept(problems);
        } else if (result instanceof IdentifierHandle id) {
            onResult.accept(id);
        } else {
            throw illegalArgException("unknown sealed type: " + result);
        }
    }


    /* Not sure if this is a good idea, it's a great "either" implementation; but
    calling it is painful because "patterns in switch" JEP seems to be stuck in
    analysis paralysis.  I'm reluctant to enable patterns since it's up to its 4th
    preview, targeted for JDK 20: https://openjdk.org/jeps/433
    Alternative would be to create a class (ends up with same conditional "if"
    handling code anyway, or throw an exception with the problems in it).
    For the moment, I'm gonna keep on with the sealed interface and hope patterns
    in switch becomes usable soon-ish.
    The "withException" methods that throw ValidationFailures is usable enough,
    maybe even better than switch would have been anyway. */
    public sealed interface ParseUrlResult permits IdentifierUrl, ParseProblems {
    }

    public sealed interface ParseHandleResult
            permits IdentifierHandle, ParseProblems {
    }

    @Getter
    public static final class ParseProblems
            implements ParseUrlResult, ParseHandleResult {
        private final List<String> problems;

        public ParseProblems(List<String> problems) {
            this.problems = problems;
        }

        public ParseProblems(String problem, String... otherProblems) {
            this.problems = new ArrayList<>();
            this.problems.add(problem);
            this.problems.addAll(Arrays.asList(otherProblems));
        }

    }

}
