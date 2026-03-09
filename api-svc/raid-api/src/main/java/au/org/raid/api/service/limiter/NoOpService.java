package au.org.raid.api.service.limiter;

import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidPatchRequest;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import org.springframework.stereotype.Service;

@Service
public class NoOpService implements RaidLimiter{
    @Override
    public boolean check(RaidCreateRequest request, Long servicePointId) {
        return true;
    }

    @Override
    public boolean check(RaidUpdateRequest request, Long servicePointId) {
        return true;
    }

    @Override
    public boolean check(RaidPatchRequest request, Long servicePointId) {
        return true;
    }
}
