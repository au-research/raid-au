package au.org.raid.api.factory;

import au.org.raid.api.dto.RaidListenerMessage;
import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.ServicePoint;
import org.springframework.stereotype.Component;

@Component
public class RaidListenerMessageFactory {
    public RaidListenerMessage create(final RaidDto raidDto, final ServicePoint servicePoint) {
        return RaidListenerMessage.builder()
                .raid(raidDto)
                .servicePoint(servicePoint)
                .build();
    }
}
