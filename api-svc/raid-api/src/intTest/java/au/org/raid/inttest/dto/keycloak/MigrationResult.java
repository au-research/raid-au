package au.org.raid.inttest.dto.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Response body of POST /realms/raid/group/migrate-service-point-admins (RAID-724). Field names
 * mirror GroupController.MigrationResult exactly.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MigrationResult {
    private int flatGroupAdminUsers;
    private int rolesCreated;
    private int grantsAdded;
    private int grantsSkipped;
    private String message;
}
