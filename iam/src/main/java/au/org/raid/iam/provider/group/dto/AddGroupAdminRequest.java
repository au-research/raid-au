package au.org.raid.iam.provider.group.dto;

import lombok.Data;

@Data
public class AddGroupAdminRequest {
    private String userId;
    private String groupId;
}
