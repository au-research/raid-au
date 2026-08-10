package au.org.raid.api.validator;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.geonames.Toponym;
import org.geonames.WebService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class GeoNamesUriValidatorTest {

    @InjectMocks
    private GeoNamesUriValidator validator;

    @Test
    @DisplayName("Returns empty list when with valid uri")
    void validUri() throws Exception {
        final var fieldId = "field-id";
        final var id = 2158177;
        final var uri = "https://www.geonames.org/%d/london.html".formatted(id);

        final var toponym = new Toponym();

        try (MockedStatic<WebService> webService = mockStatic(WebService.class)) {
            webService.when(() -> WebService.get(eq(id), isNull(), isNull())).thenReturn(toponym);

            final var failures = validator.validate(uri, fieldId);

            assertThat(failures, is(Collections.emptyList()));
        }
    }

    @Test
    @DisplayName("Returns validation failure if location not found")
    void invalidLocation() throws Exception {
        final var fieldId = "field-id";
        final var id = 2158177;
        final var uri = "https://www.geonames.org/%d/london.html".formatted(id);

        try (MockedStatic<WebService> webService = mockStatic(WebService.class)) {
            webService.when(() -> WebService.get(eq(id), isNull(), isNull())).thenThrow(new FileNotFoundException());
            final var failures = validator.validate(uri, fieldId);
            assertThat(failures, is(List.of(new ValidationFailure()
                    .fieldId(fieldId)
                    .errorType("invalidValue")
                    .message("uri not found"))));
        }
    }

    @Test
    @DisplayName("RAID-809: throws ResolverUnavailableException (resolver=GeoNames) when the GeoNames client errors")
    void serverError() throws Exception {
        final var fieldId = "field-id";
        final var id = 2158177;
        final var uri = "https://www.geonames.org/%d/london.html".formatted(id);

        try (MockedStatic<WebService> webService = mockStatic(WebService.class)) {
            webService.when(() -> WebService.get(eq(id), isNull(), isNull())).thenThrow(new IOException());

            final var e = assertThrows(ResolverUnavailableException.class,
                    () -> validator.validate(uri, fieldId));

            assertThat(e.getUnavailableResolvers(), hasSize(1));
            final var unavailable = e.getUnavailableResolvers().get(0);
            assertThat(unavailable.getField(), is(fieldId));
            assertThat(unavailable.getValue(), is(uri));
            assertThat(unavailable.getResolver(), is("GeoNames"));
            assertThat(unavailable.getDownstreamStatus(), nullValue());
        }
    }
}