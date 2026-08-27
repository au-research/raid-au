package au.org.raid.iam.provider.credential.dto;

import lombok.Data;

@Data
public class CreateCredentialRequest {
    private String groupId;
    private String label;
}
