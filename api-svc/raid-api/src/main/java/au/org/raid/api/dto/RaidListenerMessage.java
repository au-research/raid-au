package au.org.raid.api.dto;

import au.org.raid.idl.raidv2.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RaidListenerMessage {
    private RaidDto raid;
    private ServicePoint servicePoint;
}

