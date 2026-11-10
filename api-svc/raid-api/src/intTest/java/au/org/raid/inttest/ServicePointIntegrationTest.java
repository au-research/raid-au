package au.org.raid.inttest;

import au.org.raid.idl.raidv2.api.ServicePointApi;
import au.org.raid.idl.raidv2.model.ServicePointUpdateRequest;
import au.org.raid.inttest.dto.UserContext;
import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ServicePointIntegrationTest extends AbstractIntegrationTest {

    @Value("${raid.test.api.url}")
    private String apiUrl;

    private UserContext operatorContext;
    private ServicePointApi operatorServicePointApi;

    @BeforeEach
    void setupOperator() {
        operatorContext = userService.createUser("raid-au", "operator", "service-point-user");
        operatorServicePointApi = testClient.servicePointApi(operatorContext.getToken());
    }

    @AfterEach
    void tearDownOperator() {
        userService.deleteUser(operatorContext.getId());
    }

    @Test
    @DisplayName("Find all service points returns a non-empty list")
    void findAllServicePoints() {
        final var response = operatorServicePointApi.findAllServicePoints();
        final var servicePoints = response.getBody();

        assertThat(servicePoints).isNotNull();
        assertThat(servicePoints).isNotEmpty();
    }

    @Test
    @DisplayName("Find service point by ID returns the correct service point")
    void findServicePointById() {
        final var response = operatorServicePointApi.findServicePointById(raidAuRegistry2ServicePointId());
        final var servicePoint = response.getBody();

        assertThat(servicePoint).isNotNull();
        assertThat(servicePoint.getId()).isEqualTo(raidAuRegistry2ServicePointId());
        assertThat(servicePoint.getName()).isNotBlank();
    }

    @Test
    @DisplayName("Update service point preserves DataCite fields not in update request")
    void updatePreservesDataCiteFields() {
        // Read the existing service point
        final var existing = operatorServicePointApi.findServicePointById(raidAuRegistry2ServicePointId()).getBody();
        assertThat(existing).isNotNull();

        final var originalName = existing.getName();
        final var originalRepositoryId = existing.getRepositoryId();
        final var originalPrefix = existing.getPrefix();

        // Update with the same values (repositoryId, prefix, password are not in the update request)
        final var updateRequest = new ServicePointUpdateRequest()
                .id(existing.getId())
                .name(existing.getName())
                .identifierOwner(existing.getIdentifierOwner())
                .adminEmail(existing.getAdminEmail())
                .techEmail(existing.getTechEmail())
                .enabled(existing.getEnabled())
                .appWritesEnabled(existing.getAppWritesEnabled())
                .groupId(existing.getGroupId());

        final var updated = operatorServicePointApi
                .updateServicePoint(existing.getId(), updateRequest).getBody();

        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo(originalName);
        assertThat(updated.getRepositoryId()).isEqualTo(originalRepositoryId);
        assertThat(updated.getPrefix()).isEqualTo(originalPrefix);
    }

    @Test
    @DisplayName("Update service point changes editable fields")
    void updateChangesEditableFields() {
        final var existing = operatorServicePointApi.findServicePointById(raidAuRegistry2ServicePointId()).getBody();
        assertThat(existing).isNotNull();

        final var originalAdminEmail = existing.getAdminEmail();
        final var updatedAdminEmail = "updated-" + System.currentTimeMillis() + "@test.com";

        final var updateRequest = new ServicePointUpdateRequest()
                .id(existing.getId())
                .name(existing.getName())
                .identifierOwner(existing.getIdentifierOwner())
                .adminEmail(updatedAdminEmail)
                .techEmail(existing.getTechEmail())
                .enabled(existing.getEnabled())
                .appWritesEnabled(existing.getAppWritesEnabled())
                .groupId(existing.getGroupId());

        final var updated = operatorServicePointApi
                .updateServicePoint(existing.getId(), updateRequest).getBody();

        assertThat(updated).isNotNull();
        assertThat(updated.getAdminEmail()).isEqualTo(updatedAdminEmail);

        // Restore original value
        final var restoreRequest = new ServicePointUpdateRequest()
                .id(existing.getId())
                .name(existing.getName())
                .identifierOwner(existing.getIdentifierOwner())
                .adminEmail(originalAdminEmail)
                .techEmail(existing.getTechEmail())
                .enabled(existing.getEnabled())
                .appWritesEnabled(existing.getAppWritesEnabled())
                .groupId(existing.getGroupId());

        operatorServicePointApi.updateServicePoint(existing.getId(), restoreRequest);
    }

    @Test
    @DisplayName("raid-dumper role can read service points (needed for the service-points.json archive dump)")
    void raidDumperCanFindAllServicePoints() {
        final var dumperContext = userService.createUser("raid-au", "raid-dumper");

        try {
            final var response = testClient.servicePointApi(dumperContext.getToken()).findAllServicePoints();
            final var servicePoints = response.getBody();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(servicePoints).isNotNull();
            assertThat(servicePoints).isNotEmpty();
        } finally {
            userService.deleteUser(dumperContext.getId());
        }
    }

    @Test
    @DisplayName("Caller without service-point-user, operator or raid-dumper role cannot read service points")
    void insufficientRoleCannotFindAllServicePoints() {
        final var unauthorisedContext = userService.createUser("raid-au", "raid-user");

        try {
            testClient.servicePointApi(unauthorisedContext.getToken()).findAllServicePoints();
            fail("Expected findAllServicePoints to be forbidden for a caller without an authorised role");
        } catch (FeignException e) {
            assertThat(e.status()).isEqualTo(403);
        } finally {
            userService.deleteUser(unauthorisedContext.getId());
        }
    }

    // ---- GET /service-point/public (RAID-1021) ----

    @ParameterizedTest(name = "Public service points: {0} gets 200")
    @ValueSource(strings = {"raid-dumper", "service-point-user", "operator"})
    void publicServicePointsAllowedRoles(final String role) {
        final var context = userService.createUser("raid-au", role);
        try {
            final var response = testClient.servicePointApi(context.getToken()).findAllPublicServicePoints();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotEmpty();
        } finally {
            userService.deleteUser(context.getId());
        }
    }

    @Test
    @DisplayName("Public service points: no token returns 401")
    void publicServicePointsUnauthenticated() throws Exception {
        final var response = rawGet("/service-point/public", null);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Public service points: caller without an allowed role gets 403")
    void publicServicePointsInsufficientRole() {
        final var context = userService.createUser("raid-au", "raid-user");

        try {
            testClient.servicePointApi(context.getToken()).findAllPublicServicePoints();
            fail("Expected 403");
        } catch (FeignException e) {
            assertThat(e.status()).isEqualTo(403);
        } finally {
            userService.deleteUser(context.getId());
        }
    }

    @Test
    @DisplayName("Public service points: items contain only id, name and identifierOwner, with no contact details")
    void publicServicePointsContainOnlyPublicFields() throws Exception {
        final var context = userService.createUser("raid-au", "raid-dumper");

        try {
            final var response = rawGet("/service-point/public", context.getToken());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).doesNotContain("adminEmail").doesNotContain("techEmail");

            final var items = objectMapper.readTree(response.body());
            assertThat(items.isArray()).isTrue();
            assertThat(items).isNotEmpty();
            for (final var item : items) {
                final var fieldNames = new java.util.HashSet<String>();
                item.fieldNames().forEachRemaining(fieldNames::add);
                assertThat(fieldNames).isEqualTo(Set.of("id", "name", "identifierOwner"));
            }
        } finally {
            userService.deleteUser(context.getId());
        }
    }

    @Test
    @DisplayName("GET /service-point/{id} still routes to findServicePointById for a real id")
    void findByIdStillRoutesAfterPublicEndpoint() {
        final var response = operatorServicePointApi.findServicePointById(raidAuRegistry2ServicePointId());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(raidAuRegistry2ServicePointId());
        assertThat(response.getBody().getAdminEmail()).isNotNull();
    }

    private HttpResponse<String> rawGet(final String path, final String token) throws Exception {
        final var builder = HttpRequest.newBuilder(URI.create(apiUrl + path)).GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
