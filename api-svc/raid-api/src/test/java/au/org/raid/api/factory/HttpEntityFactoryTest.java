package au.org.raid.api.factory;

import au.org.raid.api.model.datacite.doi.DataciteRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpEntityFactoryTest {
    @Mock
    private HttpHeadersFactory httpHeadersFactory;
    @InjectMocks
    private HttpEntityFactory httpEntityFactory;

    @Test
    @DisplayName("Creates HttpEntity with basic auth headers")
    void create() {
        final var repositoryId = "repository-id";
        final var password = "_password";
        final var body = new DataciteRequest();

        final var headers = new HttpHeaders();
        when(httpHeadersFactory.createBasicAuthHeaders(repositoryId, password)).thenReturn(headers);

        final var result = httpEntityFactory.create(body, repositoryId, password);

        assertThat(result.getBody(), is(body));
        assertThat(result.getHeaders(), is(headers));
    }
}