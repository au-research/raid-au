package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.OrganisationRole;
import org.springframework.stereotype.Component;

@Component
public class OrganisationRoleFactory {
    public OrganisationRole create(final String id, final String schemaUri, final String startDate, final String endDate) {
        return new OrganisationRole()
                .id(id)
                .schemaUri(schemaUri)
                .startDate(startDate)
                .endDate(endDate);
    }
}