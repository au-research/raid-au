package au.org.raid.api.client.contributor.orcid;

import au.org.raid.api.client.contributor.ContributorClient;
import au.org.raid.api.dto.orcid.OrcidStringValue;
import au.org.raid.api.dto.orcid.PersonalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class OrcidClient implements ContributorClient {
    private final OrcidRequestEntityFactory requestEntityFactory;
    private final RestTemplate restTemplate;

    public PersonalDetails getPersonalDetails(final String orcid) {
        final var request = requestEntityFactory.createGetPersonalDetailsRequest(orcid);

        final var response = restTemplate.exchange(request, PersonalDetails.class);

        return response.getBody();
    }


    @Cacheable(value="valid-orcids", key="{#orcid}")
    public boolean exists(final String orcid) {
        final var request = requestEntityFactory.createHeadRequest(orcid);

        try {
            restTemplate.exchange(request, PersonalDetails.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw e;
        }
        return true;
    }


    @Cacheable(value="orcid-name-cache", key="{#orcid}")
    public String getName(final String orcid) {
        final var personalDetails = getPersonalDetails(orcid);

        final var givenNames = Optional.ofNullable(personalDetails.getName().getGivenNames())
                .map(OrcidStringValue::getValue)
                .orElse(null);
        final var familyName = Optional.ofNullable(personalDetails.getName().getFamilyName())
                .map(OrcidStringValue::getValue)
                .orElse(null);

        return Stream.of(givenNames, familyName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }
}