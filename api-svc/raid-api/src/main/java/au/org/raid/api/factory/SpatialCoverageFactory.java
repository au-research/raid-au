package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.SpatialCoverage;
import au.org.raid.idl.raidv2.model.SpatialCoveragePlace;
import au.org.raid.idl.raidv2.model.SpatialCoverageSchemaUriEnum;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpatialCoverageFactory {
    public SpatialCoverage create(final String id, final String schemaUri, final List<SpatialCoveragePlace> places) {
        return new SpatialCoverage()
                .id(id)
                .schemaUri(SpatialCoverageSchemaUriEnum.fromValue(schemaUri))
                .place(places);

    }
}