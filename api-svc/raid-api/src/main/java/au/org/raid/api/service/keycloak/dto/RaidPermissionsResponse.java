package au.org.raid.api.service.keycloak.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RaidPermissionsResponse {
    private List<String> userRaids;
    private List<String> adminRaids;
}
