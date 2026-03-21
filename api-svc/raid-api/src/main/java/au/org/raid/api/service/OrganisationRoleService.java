package au.org.raid.api.service;

import au.org.raid.api.exception.OrganisationRoleNotFoundException;
import au.org.raid.api.exception.OrganisationRoleSchemaNotFoundException;
import au.org.raid.api.factory.OrganisationRoleFactory;
import au.org.raid.api.factory.record.RaidOrganisationRecordFactory;
import au.org.raid.api.factory.record.RaidOrganisationRoleRecordFactory;
import au.org.raid.api.repository.OrganisationRoleRepository;
import au.org.raid.api.repository.OrganisationRoleSchemaRepository;
import au.org.raid.api.repository.RaidOrganisationRoleRepository;
import au.org.raid.idl.raidv2.model.ContributorRole;
import au.org.raid.idl.raidv2.model.OrganisationRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrganisationRoleService {
    private final OrganisationRoleRepository organisationRoleRepository;
    private final OrganisationRoleSchemaRepository organisationRoleSchemaRepository;
    private final RaidOrganisationRoleRepository raidOrganisationRoleRepository;
    private final RaidOrganisationRoleRecordFactory raidOrganisationRoleRecordFactory;
    private final OrganisationRoleFactory organisationRoleFactory;

    public void create(final OrganisationRole organisationRole, final int raidOrganisationId) {
        final var organisationRoleSchemaRecord =
                organisationRoleSchemaRepository.findByUri(organisationRole.getSchemaUri())
                        .orElseThrow(() -> new OrganisationRoleSchemaNotFoundException(organisationRole.getSchemaUri()));

        final var organisationRoleRecord = organisationRoleRepository.findByUriAndSchemaId(organisationRole.getId(), organisationRoleSchemaRecord.getId())
                .orElseThrow(() ->
                        new OrganisationRoleNotFoundException(organisationRole.getId(), organisationRole.getSchemaUri()));

        final var raidOrganisationRoleRecord = raidOrganisationRoleRecordFactory.create(
                raidOrganisationId,
                organisationRoleRecord.getId(),
                organisationRole.getStartDate(),
                organisationRole.getEndDate()
        );

        raidOrganisationRoleRepository.create(raidOrganisationRoleRecord);
    }

    public List<OrganisationRole> findAllByRaidOrganisationId(final Integer raidOrganisationId) {
        final var organisationRoles = new ArrayList<OrganisationRole>();

        final var raidOrganisationRoles = raidOrganisationRoleRepository.findAllByRaidOrganisationId(raidOrganisationId);

        for (final var raidOrganisationRole : raidOrganisationRoles) {
            final var organisationRoleId = raidOrganisationRole.getOrganisationRoleId();

            final var organisationRoleRecord = organisationRoleRepository
                    .findById(raidOrganisationRole.getOrganisationRoleId())
                    .orElseThrow(() -> new OrganisationRoleNotFoundException(organisationRoleId));

            final var schemaId = organisationRoleRecord.getSchemaId();

            final var organisationRoleSchemaRecord = organisationRoleSchemaRepository.findById(schemaId)
                    .orElseThrow(() -> new OrganisationRoleSchemaNotFoundException(schemaId));

            organisationRoles.add(organisationRoleFactory.create(
                    organisationRoleRecord.getUri(),
                    organisationRoleSchemaRecord.getUri(),
                    raidOrganisationRole.getStartDate(),
                    raidOrganisationRole.getEndDate()
            ));
        }
        return organisationRoles;
    }
}
