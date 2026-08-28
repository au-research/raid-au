package au.org.raid.inttest.dto.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * RAID-848: tolerant of unknown fields. Keycloak 26.6.2 returns {@code userProfileMetadata} on the
 * users endpoint, which this DTO does not model, and without this any such addition by a future
 * Keycloak version breaks deserialisation outright.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KeycloakUser {
    private String id;
    private String username;
    private boolean emailVerified;
    private Map<String, List<String>> attributes;
    private long createdTimestamp;
    private boolean enabled;
    private boolean totp;
    private List<Object> disableableCredentialTypes;
    private List<Object> requiredActions;
    private long notBefore;
    private KeycloakAccess access;

}
