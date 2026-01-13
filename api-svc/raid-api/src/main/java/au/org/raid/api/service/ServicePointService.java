package au.org.raid.api.service;

import au.org.raid.api.client.repository.DataciteRepositoryClient;
import au.org.raid.api.factory.ServicePointFactory;
import au.org.raid.api.factory.record.ServicePointRecordFactory;
import au.org.raid.api.model.datacite.repository.DataciteRepositoryFactory;
import au.org.raid.api.repository.ServicePointRepository;
import au.org.raid.idl.raidv2.model.ServicePoint;
import au.org.raid.idl.raidv2.model.ServicePointCreateRequest;
import au.org.raid.idl.raidv2.model.ServicePointUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class ServicePointService {
    private final ServicePointRepository servicePointRepository;
    private final ServicePointRecordFactory servicePointRecordFactory;
    private final ServicePointFactory servicePointFactory;
    private final RepositoryService repositoryService;

    public ServicePoint create(final ServicePointCreateRequest servicePoint) {
        final var password = RandomStringUtils.insecure().nextAlphanumeric(20);

        final var repository = repositoryService.create(servicePoint.getName(), servicePoint.getAdminEmail(), password);

        servicePoint.setPassword(password);
        servicePoint.setPrefix(repository.getData().getRelationships().getPrefixes().getData().get(0).getId());
        servicePoint.setRepositoryId(repository.getData().getAttributes().getSymbol());

        final var record = servicePointRecordFactory.create(servicePoint);
        return servicePointFactory.create(servicePointRepository.create(record));
    }

    public ServicePoint update(final ServicePointUpdateRequest servicePoint) {
        final var record = servicePointRecordFactory.create(servicePoint);
        return servicePointFactory.create(servicePointRepository.update(record));
    }

    public Optional<ServicePoint> findById(final Long id) {
        return servicePointRepository.findById(id)
                .map(servicePointFactory::create)
                .or(Optional::empty);
    }

    public List<ServicePoint> findAll() {
        return servicePointRepository.findAll().stream()
                .map(servicePointFactory::create)
                .toList();
    }

    public Optional<ServicePoint> findByGroupId(final String groupId) {
        return servicePointRepository.findByGroupId(groupId)
                .map(servicePointFactory::create)
                .or(Optional::empty);

    }
}
