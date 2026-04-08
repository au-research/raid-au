package au.org.raid.api.service.limiter;

import au.org.raid.idl.raidv2.model.BudgetFailure;
import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidPatchRequest;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;

import java.util.List;

public interface RaidLimiter {

    List<BudgetFailure> check(RaidCreateRequest request, Long servicePointId);
    List<BudgetFailure> check(RaidUpdateRequest request, Long servicePointId);
    List<BudgetFailure> check(RaidPatchRequest request, Long servicePointId);
}
