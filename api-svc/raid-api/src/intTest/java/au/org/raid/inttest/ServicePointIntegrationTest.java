package au.org.raid.inttest;

import au.org.raid.idl.raidv2.api.ServicePointApi;
import au.org.raid.idl.raidv2.model.ServicePoint;
import au.org.raid.idl.raidv2.model.ServicePointUpdateRequest;
import au.org.raid.inttest.dto.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ServicePointIntegrationTest extends AbstractIntegrationTest {

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
        final var response = operatorServicePointApi.findServicePointById(UQ_SERVICE_POINT_ID);
        final var servicePoint = response.getBody();

        assertThat(servicePoint).isNotNull();
        assertThat(servicePoint.getId()).isEqualTo(UQ_SERVICE_POINT_ID);
        assertThat(servicePoint.getName()).isNotBlank();
    }

    @Test
    @DisplayName("Update service point preserves DataCite fields not in update request")
    void updatePreservesDataCiteFields() {
        // Read the existing service point
        final var existing = operatorServicePointApi.findServicePointById(UQ_SERVICE_POINT_ID).getBody();
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
        final var existing = operatorServicePointApi.findServicePointById(UQ_SERVICE_POINT_ID).getBody();
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
}
