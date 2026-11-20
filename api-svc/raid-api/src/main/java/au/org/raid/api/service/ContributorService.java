package au.org.raid.api.service;

import au.org.raid.api.exception.ContributorNotFoundException;
import au.org.raid.api.exception.ContributorSchemaNotFoundException;
import au.org.raid.api.factory.ContributorFactory;
import au.org.raid.api.factory.record.ContributorRecordFactory;
import au.org.raid.api.factory.record.RaidContributorRecordFactory;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.api.repository.ContributorSchemaRepository;
import au.org.raid.api.repository.RaidContributorRepository;
import au.org.raid.db.jooq.tables.records.ContributorRecord;
import au.org.raid.idl.raidv2.model.Contributor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
            final var contributorSchema = contributorSchemaRepository.findByUri(contributor.getSchemaUri().getValue())
                    .orElseThrow(() -> new ContributorSchemaNotFoundException(contributor.getSchemaUri().getValue()));

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

    /**
     * Replaces each contributor's status with the one stored for that person, or UNAUTHENTICATED when
     * none is stored. A status sent by the client is never kept: only the ORCID updater sets status,
     * through {@link #updateStatus}.
     */
    public void setStatus(final List<Contributor> contributors) {
        if (contributors == null) {
            return;
        }

        contributors.forEach(contributor -> contributor.setStatus(storedStatus(contributor.getId())));
    }

    /**
     * Drops each contributor's email. The field is deprecated and ignored: the API never stores it,
     * and a client that still sends it isn't refused.
     */
    public void dropEmail(final List<Contributor> contributors) {
        if (contributors == null) {
            return;
        }

        contributors.forEach(contributor -> contributor.setEmail(null));
    }

    /**
     * Sets the status stored for a contributor. The row is shared by every RAiD the person is on.
     *
     * @return false when no contributor has this id
     */
    public boolean updateStatus(final String pid, final String status) {
        return contributorRepository.updateStatus(pid, status) > 0;
    }

    private String storedStatus(final String pid) {
        if (isBlank(pid)) {
            return UNAUTHENTICATED_STATUS;
        }

        return contributorRepository.findByPid(pid)
                .map(ContributorRecord::getStatus)
                .filter(status -> !isBlank(status))
                .orElse(UNAUTHENTICATED_STATUS);
    }
}
