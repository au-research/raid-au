package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.inttest.service.Handle;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class RaidAllEndpointIntegrationTest extends AbstractIntegrationTest {

    @Value("${raid.test.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    @DisplayName("GET /raid/all returns raids for operator user")
    void findAllRaidsAsOperator() {
        final var operatorContext = userService.createUser("raid-au", "service-point-user", "operator");

        try {
            // Mint a raid so there's at least one to find
            final var mintedRaid = raidApi.mintRaid(createRequest).getBody();
            assert mintedRaid != null;

            final var headers = new HttpHeaders();
            headers.setBearerAuth(operatorContext.getToken());

            final var response = restTemplate.exchange(
                    apiUrl + "/raid/all",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<RaidDto>>() {}
            );

            assertThat(response.getStatusCode(), is(HttpStatus.OK));
            assertThat(response.getBody(), is(notNullValue()));
            assertThat(response.getBody().size(), is(greaterThanOrEqualTo(1)));

            // Verify the minted raid appears in the results
            final var raidIds = response.getBody().stream()
                    .map(r -> r.getIdentifier().getId())
                    .toList();
            assertThat(raidIds, hasItem(mintedRaid.getIdentifier().getId()));
        } finally {
            userService.deleteUser(operatorContext.getId());
        }
    }

    @Test
    @DisplayName("GET /raid/all returns 403 for non-operator user")
    void findAllRaidsAsNonOperator() {
        // The default userContext from AbstractIntegrationTest has service-point-user role, not operator
        final var headers = new HttpHeaders();
        headers.setBearerAuth(userContext.getToken());

        try {
            restTemplate.exchange(
                    apiUrl + "/raid/all",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<RaidDto>>() {}
            );
            fail("Expected 403 Forbidden but got 200");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode(), is(HttpStatus.FORBIDDEN));
        }
    }
}
