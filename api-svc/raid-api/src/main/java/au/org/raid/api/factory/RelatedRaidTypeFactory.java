package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.RelatedRaidType;
import org.springframework.stereotype.Component;

@Component
public class RelatedRaidTypeFactory {
    public RelatedRaidType create(final String id, final String schemaUri) {
        return new RelatedRaidType()
                .id(id)
                .schemaUri(schemaUri);
    }
}
