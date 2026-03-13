package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteDto;
import au.org.raid.api.model.datacite.doi.DataciteRequest;
import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataciteRequestFactory {
    private final DataciteDtoFactory dataciteDtoFactory;
    public DataciteRequest create(RaidCreateRequest createRequest, String handle) {
        DataciteDto dataciteDto = dataciteDtoFactory.create(createRequest, handle);

        return new DataciteRequest()
                .setData(dataciteDto);
    }

    public DataciteRequest create(RaidUpdateRequest updateRequest, String handle) {
        DataciteDto dataciteDto = dataciteDtoFactory.create(updateRequest, handle);

        return new DataciteRequest()
                .setData(dataciteDto);
    }

    public DataciteRequest create(RaidDto updateRequest, String handle) {
        DataciteDto dataciteDto = dataciteDtoFactory.create(updateRequest, handle);

        return new DataciteRequest()
                .setData(dataciteDto);
    }
}
