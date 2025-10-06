package au.org.raid.iam.provider.group.dto;

import lombok.Data;

@Data
public class GroupLeaveRequest {
    private String groupId;
    private String userId;
}