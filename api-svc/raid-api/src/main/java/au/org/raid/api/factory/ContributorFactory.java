package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ContributorRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContributorFactory {
    public Contributor create(final String id, final String schemaUri, final Boolean leader, final Boolean contact, final List<ContributorPosition> positions, final List<ContributorRole> roles) {
        return new Contributor()
                .id(id)
                .schemaUri(schemaUri)
                .leader(leader)
                .contact(contact)
                .position(positions)
                .role(roles);
    }
}