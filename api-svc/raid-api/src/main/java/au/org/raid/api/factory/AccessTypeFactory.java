package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.AccessType;
import org.springframework.stereotype.Component;

@Component
public class AccessTypeFactory {
    public AccessType create(final String id, final String schemaUri) {
        return new AccessType()
                .id(id)
                .schemaUri(schemaUri);
    }
}
