package au.org.raid.iam.provider.group.dto;

import lombok.Data;

@Data
public class RemoveGroupAdminRequest {
    private String userId;
    private String groupId;
}
