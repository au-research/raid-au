package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteRight;
import au.org.raid.idl.raidv2.model.Access;
import au.org.raid.idl.raidv2.model.Id;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataciteRightFactory {

    public DataciteRight create(final Access raidAccess, final Id raidId) {
        if (raidAccess == null || raidId == null) {
            return null;
        }

        String license = Optional.ofNullable(raidId.getLicense()).orElse("");

        return new DataciteRight()
                .setRights(license)
                .setRightsURI(license);
    }
}
