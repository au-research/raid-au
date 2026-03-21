package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.ContributorRole;
import org.springframework.stereotype.Component;

@Component
public class ContributorRoleFactory {
    public ContributorRole create(final String id, final String schemaUri) {
        return new ContributorRole()
                .id(id)
                .schemaUri(schemaUri);
    }
}