package au.org.raid.api.factory;

import au.org.raid.api.dto.RaidListenerMessage;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.Id;
import au.org.raid.idl.raidv2.model.Title;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RaidListenerMessageFactory {
    public RaidListenerMessage create(final Id id, final Contributor contributor, final List<Title> title) {
        return RaidListenerMessage.builder()
                .identifier(id)
                .contributor(contributor)
                .title(title)
                .build();
    }
}
