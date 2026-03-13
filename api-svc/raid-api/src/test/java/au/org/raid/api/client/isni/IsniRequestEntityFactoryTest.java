package au.org.raid.api.client.isni;

import au.org.raid.api.client.contributor.isni.IsniRequestEntityFactory;
import au.org.raid.api.config.properties.IsniClientProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IsniRequestEntityFactoryTest {
    @Mock
    private IsniClientProperties properties;
    @InjectMocks
    private IsniRequestEntityFactory requestEntityFactory;

    @Test
    @DisplayName("Should return RequestEntity with valid input")
    void returnsRequestEntity() {
        final var urlFormat = "https://localhost/__ISNI__/details";
        final var ror = "https://isni.org/isni/0000000078519858";

        when(properties.getUrlFormat()).thenReturn(urlFormat);

        final var request = requestEntityFactory.create(ror);

        assertThat(request.getMethod(), is(HttpMethod.GET));
        assertThat(request.getUrl(), is(URI.create("https://localhost/0000000078519858/details")));
    }
}