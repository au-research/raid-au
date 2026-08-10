package au.org.raid.api.validator;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.idl.raidv2.model.UnavailableResolver;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geonames.WebService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;

@Slf4j
@Getter
@RequiredArgsConstructor
public class GeoNamesUriValidator implements UriValidator {
    private final RestTemplate restTemplate;
    private final String regex = "^https://(www\\.)?geonames.org/\\d+/.*$";

    @Value("${raid.validation.geonames.username}")
    private final String username;
    @Override
    public List<ValidationFailure> validate(String uri, String fieldId) {
        final var failures = new ArrayList<ValidationFailure>();

        if (!uri.matches(regex)) {
            failures.add(new ValidationFailure()
                    .fieldId(fieldId)
                    .errorType(INVALID_VALUE_TYPE)
                    .message(INVALID_VALUE_MESSAGE + " - should match " + regex)
            );
        } else {
            final var pattern = Pattern.compile("^[^\\d]+(\\d+)[^\\d]+$");
            Matcher matcher = pattern.matcher(uri);

            if (matcher.find()) {
                final var geoNameId = matcher.group(1);
                WebService.setUserName(username);

                try  {
                    final var start = Instant.now();
                    WebService.get(Integer.parseInt(geoNameId), null, null);
                    final var end = Instant.now();
                    Duration duration = Duration.between(start, end);

                    log.debug("Validated geonames id {} in {}.{} seconds", geoNameId, duration.getSeconds(), duration.getNano());
                } catch (FileNotFoundException e) {
                    failures.add(new ValidationFailure()
                            .fieldId(fieldId)
                            .errorType(INVALID_VALUE_TYPE)
                            .message(URI_DOES_NOT_EXIST)
                    );
                } catch (Exception e) {
                    // The org.geonames.WebService client doesn't use RestTemplate/RestClientException,
                    // so it can't go through AbstractUriValidator's catch/convert logic - it throws a
                    // checked Exception for anything other than a clean not-found. The resolver, not
                    // the URI, is at fault here, so this is a 503 (RAID-809), not a validation failure.
                    // downstreamStatus is left null: the geonames client doesn't expose an HTTP status.
                    log.error("Unable to validate %s".formatted(uri), e);
                    throw new ResolverUnavailableException(List.of(
                            new UnavailableResolver()
                                    .field(fieldId)
                                    .value(uri)
                                    .resolver("GeoNames")
                                    .downstreamMessage("GeoNames resolve %s -> connection failed".formatted(uri))
                    ));
                }
            }
        }

        return failures;
    }
}
