package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.ContributorRole;
import au.org.raid.idl.raidv2.model.ContributorRoleIdEnum;
import au.org.raid.idl.raidv2.model.ContributorRoleSchemaUriEnum;
import org.springframework.stereotype.Component;

@Component
public class ContributorRoleFactory {
    public ContributorRole create(final String id, final String schemaUri) {
        return new ContributorRole()
                .id(ContributorRoleIdEnum.fromValue(id))
                .schemaUri(ContributorRoleSchemaUriEnum.fromValue(schemaUri));
    }
}