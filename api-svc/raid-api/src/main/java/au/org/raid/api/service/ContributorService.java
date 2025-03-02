package au.org.raid.api.service;

import au.org.raid.api.exception.ContributorNotFoundException;
import au.org.raid.api.exception.ContributorSchemaNotFoundException;
import au.org.raid.api.factory.ContributorFactory;
import au.org.raid.api.factory.record.ContributorRecordFactory;
import au.org.raid.api.factory.record.RaidContributorRecordFactory;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.api.repository.ContributorSchemaRepository;
import au.org.raid.api.repository.RaidContributorRepository;
import au.org.raid.idl.raidv2.model.Contributor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@Transactional
@RequiredArgsConstructor
public class ContributorService {
    private static final String UNAUTHENTICATED_STATUS = "UNAUTHENTICATED";

    private final ContributorRepository contributorRepository;
    private final RaidContributorRepository raidContributorRepository;
    private final ContributorRecordFactory contributorRecordFactory;
    private final RaidContributorRecordFactory raidContributorRecordFactory;
    private final ContributorPositionService contributorPositionService;
    private final ContributorRoleService contributorRoleService;
    private final ContributorFactory contributorFactory;
    private final ContributorSchemaRepository contributorSchemaRepository;

    public void create(final List<Contributor> contributors, final String handle) {
        if (contributors == null) {
            return;
        }

        for (var contributor : contributors) {
            final var contributorSchema = contributorSchemaRepository.findByUri(contributor.getSchemaUri())
                    .orElseThrow(() -> new ContributorSchemaNotFoundException(contributor.getSchemaUri()));

            final var contributorRecord = contributorRecordFactory.create(contributor, contributorSchema.getId());
            final var contributorId = contributorRepository.updateOrCreate(contributorRecord).getId();

            final var raidContributorRecord = raidContributorRecordFactory.create(contributor, contributorId, handle);
            final var raidContributorId = raidContributorRepository.create(raidContributorRecord).getId();

            contributorPositionService.create(contributor.getPosition(), raidContributorId);
            contributorRoleService.create(contributor.getRole(), raidContributorId);
        }
    }

    public List<Contributor> findAllByHandle(final String handle) {
        final var contributors = new ArrayList<Contributor>();

        final var records = raidContributorRepository.findAllByHandle(handle);

        for (final var record : records) {
            final var contributorId = record.getContributorId();

            final var contributorRecord = contributorRepository.findById(contributorId)
                    .orElseThrow(() -> new ContributorNotFoundException(contributorId));

            final var schemaId = contributorRecord.getSchemaId();

            final var contributorSchemaRecord = contributorSchemaRepository.findById(schemaId)
                    .orElseThrow(() -> new ContributorSchemaNotFoundException(schemaId));

            final var positions = contributorPositionService.findAllByRaidContributorId(record.getId());
            final var roles = contributorRoleService.findAllByRaidContributorId(record.getId());

            contributors.add(contributorFactory.create(
                    contributorRecord.getPid(),
                    contributorSchemaRecord.getUri(),
                    record.getLeader(),
                    record.getContact(),
                    positions,
                    roles
            ));
        }

        return contributors;
    }

    public void update(final List<Contributor> contributors, final String handle) {
        raidContributorRepository.deleteAllByHandle(handle);
        create(contributors, handle);
    }

    public void setStatusAndUuid(final List<Contributor> contributors) {

        contributors.forEach(contributor -> {
            var updateContributorRecord = false;
            if (!isBlank(contributor.getId())) {
                final var orcid = contributor.getId().substring(contributor.getId().lastIndexOf('/') + 1);
                final var optional = contributorRepository.findByPid(orcid);
                if (optional.isPresent()) {
                    final var contributorRecord = optional.get();
                    if (contributorRecord.getStatus() == null) {
                        contributor.status(UNAUTHENTICATED_STATUS);
                        contributorRecord.setStatus(UNAUTHENTICATED_STATUS);
                        updateContributorRecord = true;
                    } else {
                        contributor.status(contributorRecord.getStatus());
                    }

                    if (contributorRecord.getUuid() == null){
                        final var uuid = UUID.randomUUID().toString();
                        contributor.uuid(uuid);
                        contributorRecord.setUuid(uuid);
                        updateContributorRecord = true;
                    } else {
                        contributor.uuid(contributorRecord.getUuid());
                    }

                    if (updateContributorRecord) {
                        contributorRepository.update(contributorRecord);
                    }
                } else {
                    contributor.uuid(UUID.randomUUID().toString());
                    contributor.status(UNAUTHENTICATED_STATUS);
                }
            }
        });
    }
}
