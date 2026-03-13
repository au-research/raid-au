package au.org.raid.api.client.contributor.orcid;

import au.org.raid.api.config.properties.OrcidClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class OrcidRequestEntityFactory {
    private final OrcidClientProperties properties;

    public RequestEntity<Void> createGetPersonalDetailsRequest(final String orcid) {
        final var id = orcid.substring(orcid.lastIndexOf("/") + 1);

        return RequestEntity.method(HttpMethod.GET, URI.create(properties.getBaseUrl() + id + "/personal-details"))
                .header("Authorization", "Bearer %s".formatted(properties.getAccessToken()))
                .build();
    }

    public RequestEntity<Void> createHeadRequest(final String orcid) {
        final var id = orcid.substring(orcid.lastIndexOf("/") + 1);

        return RequestEntity.method(HttpMethod.HEAD, URI.create(properties.getBaseUrl() + id ))
                .header("Authorization", "Bearer %s".formatted(properties.getAccessToken()))
                .build();
    }
}
