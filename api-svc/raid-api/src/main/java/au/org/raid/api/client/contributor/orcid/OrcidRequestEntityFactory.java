package au.org.raid.api.client.contributor.orcid;

import au.org.raid.api.config.properties.OrcidClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class OrcidRequestEntityFactory {
    /**
     * Placeholder default for {@code raid.orcid-client.access-token} in application.yaml.
     * Treated the same as no token at all.
     */
    static final String TOKEN_NOT_SET = "not-set";

    private final OrcidClientProperties properties;

    public RequestEntity<Void> createGetPersonalDetailsRequest(final String orcid) {
        final var id = orcid.substring(orcid.lastIndexOf("/") + 1);

        return withAuthorization(RequestEntity.method(HttpMethod.GET, URI.create(properties.getBaseUrl() + id + "/personal-details")))
                .build();
    }

    public RequestEntity<Void> createHeadRequest(final String orcid) {
        final var id = orcid.substring(orcid.lastIndexOf("/") + 1);

        return withAuthorization(RequestEntity.method(HttpMethod.HEAD, URI.create(properties.getBaseUrl() + id)))
                .build();
    }

    /**
     * Adds the bearer token only when one is configured. The ORCID public API
     * (pub.orcid.org) serves anonymous reads, but rejects a placeholder token with
     * 401, so with no token configured the request must carry no Authorization
     * header at all (RAID-1031).
     */
    private RequestEntity.BodyBuilder withAuthorization(final RequestEntity.BodyBuilder builder) {
        final var token = properties.getAccessToken();
        if (token == null || token.isBlank() || TOKEN_NOT_SET.equals(token.trim())) {
            return builder;
        }
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(token));
    }
}
