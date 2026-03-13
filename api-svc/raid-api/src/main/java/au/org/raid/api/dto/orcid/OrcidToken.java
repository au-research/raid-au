package au.org.raid.api.dto.orcid;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrcidToken {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private Long expiresIn;

    private String scope;

    private String orcid;
}
