package au.org.raid.api.factory;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HttpEntityFactory {
    private final HttpHeadersFactory httpHeadersFactory;

    public <T> HttpEntity<T> create(final T body, final String username, final String password) {
        final HttpHeaders headers = httpHeadersFactory.createBasicAuthHeaders(username, password);
        return new HttpEntity<>(body, headers);
    }


    public <T> HttpEntity<T> create(final T body, final String apiKey) {
        final var headers = new HttpHeaders();
        headers.set("Content-type", "application/json");
        headers.set("X-API-Key", apiKey);

        return new HttpEntity<>(body, headers);
    }
}
