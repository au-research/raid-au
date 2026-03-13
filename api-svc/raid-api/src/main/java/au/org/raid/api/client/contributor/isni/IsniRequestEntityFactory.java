package au.org.raid.api.client.contributor.isni;

import au.org.raid.api.config.properties.IsniClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class IsniRequestEntityFactory {
    private final IsniClientProperties properties;

    public RequestEntity<Void> create(final String isni) {
        final var id = isni.substring(isni.lastIndexOf("/") + 1);

        final var uri = properties.getUrlFormat().replace("__ISNI__", id);

        return RequestEntity.method(HttpMethod.GET, URI.create(uri))
                .build();
    }
}
