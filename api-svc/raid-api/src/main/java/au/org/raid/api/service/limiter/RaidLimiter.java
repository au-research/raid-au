package au.org.raid.api.service.limiter;

import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidPatchRequest;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;

public interface RaidLimiter {

    boolean check(RaidCreateRequest request, Long servicePointId);
    boolean check(RaidUpdateRequest request, Long servicePointId);
    boolean check(RaidPatchRequest request, Long servicePointId);
}
