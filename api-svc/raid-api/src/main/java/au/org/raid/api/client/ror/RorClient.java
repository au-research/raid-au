package au.org.raid.api.client.ror;

import au.org.raid.api.client.ror.dto.RorSchemaV21;
import au.org.raid.api.client.ror.dto.Type;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class RorClient {
    private final RestTemplate restTemplate;
    private final RorRequestEntityFactory requestEntityFactory;

    public RorClient(
            @Qualifier("uriValidatorRestTemplate") final RestTemplate restTemplate,
            final RorRequestEntityFactory requestEntityFactory
    ) {
        this.restTemplate = restTemplate;
        this.requestEntityFactory = requestEntityFactory;
    }

    public RorSchemaV21 getOrganisation(final String ror) {
        final var request = requestEntityFactory.create(ror);
        final var response = restTemplate.exchange(request, RorSchemaV21.class);
        return response.getBody();
    }

    @Cacheable(value="organisation-name-cache", key="{#id}")
    public String getOrganisationName(final String id) {
        log.debug("Calling getOrganisationName");
        final var organisation = this.getOrganisation(id);

        final var name = organisation.getNames().stream()
                .filter(n -> n.getTypes().contains(Type.ROR_DISPLAY))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("ROR display name not found"));

        return name.getValue();
    }

    @Cacheable(value="valid-ror", key="{#ror}")
    public boolean exists(final String ror) {
        try {
            getOrganisation(ror);
        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw e;
        }
        return true;
    }

}
