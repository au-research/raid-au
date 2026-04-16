package au.org.raid.api.service;

import au.org.raid.api.exception.ContributorRoleNotFoundException;
import au.org.raid.api.exception.ContributorRoleSchemaNotFoundException;
import au.org.raid.api.factory.ContributorPositionFactory;
import au.org.raid.api.factory.ContributorRoleFactory;
import au.org.raid.api.factory.record.RaidContributorRoleRecordFactory;
import au.org.raid.api.repository.*;
import au.org.raid.db.jooq.tables.records.RaidContributorRoleRecord;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ContributorRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ContributorRoleService {
    private final RaidContributorRoleRepository raidContributorRoleRepository;
    private final RaidContributorRoleRecordFactory raidContributorRoleRecordFactory;
    private final ContributorRoleFactory contributorRoleFactory;
    private final ContributorRoleRepository contributorRoleRepository;
    private final ContributorRoleSchemaRepository contributorRoleSchemaRepository;

    public List<ContributorRole> findAllByRaidContributorId(final Integer raidContributorId) {
        final var contributorRoles = new ArrayList<ContributorRole>();

        final var raidContributorRoles = raidContributorRoleRepository.findAllByRaidContributorId(raidContributorId);

        for (final var raidContributorRole : raidContributorRoles) {
            final var contributorRoleId = raidContributorRole.getContributorRoleId();

            final var contributorRoleRecord = contributorRoleRepository
                    .findById(raidContributorRole.getContributorRoleId())
                    .orElseThrow(() -> new ContributorRoleNotFoundException(contributorRoleId));

            final var schemaId = contributorRoleRecord.getSchemaId();

            final var contributorRoleSchemaRecord = contributorRoleSchemaRepository.findById(schemaId)
                    .orElseThrow(() -> new ContributorRoleSchemaNotFoundException(schemaId));

            contributorRoles.add(contributorRoleFactory.create(
                    contributorRoleRecord.getUri(),
                    contributorRoleSchemaRecord.getUri())
            );
        }
        return contributorRoles;
    }

    public void create(final List<ContributorRole> roles, final int raidContributorId) {
        if (roles == null) {
            return;
        }

        for (final var contributorRole : roles) {
            final var roleSchema = contributorRoleSchemaRepository.findByUri(contributorRole.getSchemaUri())
                    .orElseThrow(() ->
                            new ContributorRoleSchemaNotFoundException(contributorRole.getSchemaUri()));

            final var role = contributorRoleRepository.findByUriAndSchemaId(contributorRole.getId(), roleSchema.getId())
                    .orElseThrow(() ->
                            new ContributorRoleNotFoundException(contributorRole.getId(), contributorRole.getSchemaUri()));

            final var raidContributorRoleRecord = raidContributorRoleRecordFactory.create(
                    raidContributorId, role.getId());

            raidContributorRoleRepository.create(raidContributorRoleRecord);
        }
    }
}
