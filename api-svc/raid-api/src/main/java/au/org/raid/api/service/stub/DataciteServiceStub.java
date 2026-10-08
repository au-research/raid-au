package au.org.raid.api.service.stub;

import au.org.raid.api.factory.datacite.DataciteRequestFactory;
import au.org.raid.api.service.datacite.DataciteService;
import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds the DataCite request exactly as the real service does (so payload-building failures
 * still surface) but never sends it. Temporary measure for RAID-892 so branch environments
 * stop minting DOIs in DataCite's test instance, until the RAID-812 mock server replaces it.
 */
@Slf4j
public class DataciteServiceStub extends DataciteService {
    private final DataciteRequestFactory dataciteRequestFactory;
    private final Long delayMilliseconds;

    public DataciteServiceStub(final DataciteRequestFactory dataciteRequestFactory,
                               final ObjectMapper objectMapper,
                               final Long delayMilliseconds) {
        super(null, null, dataciteRequestFactory, null, objectMapper);
        this.dataciteRequestFactory = dataciteRequestFactory;
        this.delayMilliseconds = delayMilliseconds != null ? delayMilliseconds : 0L;
    }

    @Override
    @SneakyThrows
    public void mint(final RaidCreateRequest request, final String handle,
                     final String repositoryId, final String password) {
        if (!isDoi(handle)) {
            return;
        }
        dataciteRequestFactory.create(request, handle);
        simulate("mint", handle);
    }

    @Override
    @SneakyThrows
    public void update(final RaidUpdateRequest request, final String handle,
                       final String repositoryId, final String password) {
        if (!isDoi(handle)) {
            return;
        }
        dataciteRequestFactory.create(request, handle);
        simulate("update", handle);
    }

    @Override
    @SneakyThrows
    public void update(final RaidDto request, final String handle,
                       final String repositoryId, final String password) {
        if (!isDoi(handle)) {
            return;
        }
        dataciteRequestFactory.create(request, handle);
        simulate("update", handle);
    }

    private void simulate(final String operation, final String handle) throws InterruptedException {
        Thread.sleep(delayMilliseconds);
        log.info("in-memory DataCite stub: skipped {} for {}", operation, handle);
    }
}
