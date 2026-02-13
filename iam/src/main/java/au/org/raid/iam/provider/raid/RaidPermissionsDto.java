package au.org.raid.iam.provider.raid;

import lombok.Data;

@Data
public class RaidPermissionsDto {
    private boolean servicePointMatch;
    private boolean read;
    private boolean write;
}
