package au.org.raid.api.service.limiter;

import au.org.raid.idl.raidv2.model.BudgetFailure;
import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidPatchRequest;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class NoOpService implements RaidLimiter{
    @Override
    public List<BudgetFailure> check(RaidCreateRequest request, Long servicePointId) {
        return Collections.emptyList();
    }

    @Override
    public List<BudgetFailure> check(RaidUpdateRequest request, Long servicePointId) {
        return Collections.emptyList();
    }

    @Override
    public List<BudgetFailure> check(RaidPatchRequest request, Long servicePointId) {
        return Collections.emptyList();
    }
}
