package au.org.raid.api.dto;

import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.Id;
import au.org.raid.idl.raidv2.model.Title;
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
    private Id identifier;
    private Contributor contributor;
    private List<Title> title;
}

