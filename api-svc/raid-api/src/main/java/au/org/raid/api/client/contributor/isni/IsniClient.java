package au.org.raid.api.client.contributor.isni;

import au.org.raid.api.client.contributor.ContributorClient;
import au.org.raid.api.client.isni.dto.PersonalName;
import au.org.raid.api.dto.isni.SearchRetrieveResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Element;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class IsniClient implements ContributorClient {
    private final RestTemplate restTemplate;
    private final IsniRequestEntityFactory requestEntityFactory;

    public SearchRetrieveResponse search(final String isni) {
        final var request = requestEntityFactory.create(isni);

        final var responseEntity = restTemplate.exchange(request, SearchRetrieveResponse.class);

        return  responseEntity.getBody();
    }

    @Cacheable(value="isni-name-cache", key="{#isni}")
    public String getName(final String isni) {
        final var result = search(isni);

        if (result.getNumberOfRecords() > 0) {
            final var record = result.getFirstRecord();

            final var personalNames = record.getISNIAssigned()
                    .getISNIMetadata()
                    .getIdentity()
                    .getPersonOrFiction()
                    .getPersonalName();

            if (personalNames == null || personalNames.isEmpty()) {
                throw new RuntimeException("No name found for ISNI %s".formatted(isni));
            }

            return personalNames.stream()
                    .filter(personalName -> {
                        final var nameUse = personalName.getNameUse();
                        return nameUse != null && (nameUse.equalsIgnoreCase("legal") || nameUse.equalsIgnoreCase("public"));
                    })
                    .findFirst()
                    .or(() -> personalNames.stream().findFirst())
                    .map(this::getFullName)
                    .orElseThrow(() -> new RuntimeException("No name found for ISNI %s".formatted(isni)));
        } else {
            throw new RuntimeException("ISNI not found %s".formatted(isni));
        }
    }

    private String getFullName(final PersonalName personalName) {
        // Some records - e.g. a mononymous pen name, catalogued with only a
        // surname - have no forename element at all. Join whichever of the
        // two is actually present rather than assuming both always exist.
        final var givenName = textContentOrNull(personalName.getForename());
        final var familyName = textContentOrNull(personalName.getSurname());

        return Stream.of(givenName, familyName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    private String textContentOrNull(final Object nameElement) {
        return nameElement instanceof Element element ? element.getTextContent() : null;
    }

    @Cacheable(value="valid-isni", key="{#isni}")
    public boolean exists(@NotNull String isni) {
        final var result = this.search(isni);
        return result.getNumberOfRecords() > 0;
    }
}
