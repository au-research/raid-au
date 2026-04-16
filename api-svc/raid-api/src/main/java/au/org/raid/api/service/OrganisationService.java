package au.org.raid.api.service;

import au.org.raid.api.exception.OrganisationNotFoundException;
import au.org.raid.api.exception.OrganisationSchemaNotFoundException;
import au.org.raid.api.factory.OrganisationFactory;
import au.org.raid.api.factory.record.OrganisationRecordFactory;
import au.org.raid.api.factory.record.RaidOrganisationRecordFactory;
import au.org.raid.api.repository.OrganisationRepository;
import au.org.raid.api.repository.OrganisationSchemaRepository;
import au.org.raid.api.repository.RaidOrganisationRepository;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.Organisation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class OrganisationService {
    private final OrganisationRepository organisationRepository;
    private final OrganisationSchemaRepository organisationSchemaRepository;
    private final RaidOrganisationRepository raidOrganisationRepository;
    private final OrganisationRecordFactory organisationRecordFactory;
    private final RaidOrganisationRecordFactory raidOrganisationRecordFactory;
    private final OrganisationRoleService organisationRoleService;
    private final OrganisationFactory organisationFactory;

    public void create(final List<Organisation> organisations, final String handle) {
        if (organisations == null) {
            return;
        }

        for (final var organisation : organisations) {
            final var organisationSchemaRecord = organisationSchemaRepository.findByUri(organisation.getSchemaUri())
                    .orElseThrow(() ->
                            new OrganisationSchemaNotFoundException(organisation.getSchemaUri()));

            final var organisationRecord = organisationRecordFactory.create(organisation, organisationSchemaRecord.getId());
            final var savedOrganisation = organisationRepository.findOrCreate(organisationRecord);
            final var raidOrganisationRecord = raidOrganisationRecordFactory.create(savedOrganisation.getId(), handle);

            final var savedRaidOrganisationRecord = raidOrganisationRepository.create(raidOrganisationRecord);

            for (final var role : organisation.getRole()) {
                organisationRoleService.create(role, savedRaidOrganisationRecord.getId());
            }
        }
    }

    public Integer findOrCreate(final String pid, final String schemaUri) {
        final var organisationSchemaRecord = organisationSchemaRepository.findByUri(schemaUri)
                .orElseThrow(() -> new OrganisationSchemaNotFoundException("Organisation schema not found %s".formatted(schemaUri)));

        final var organisationRecord = organisationRecordFactory.create(pid, organisationSchemaRecord.getId());

        final var organisation = organisationRepository.findOrCreate(organisationRecord);

        return organisation.getId();
    }

    public List<Organisation> findAllByHandle(final String handle) {
        final var organisations = new ArrayList<Organisation>();

        final var records = raidOrganisationRepository.findAllByHandle(handle);

        for (final var record : records) {
            final var organisationId = record.getOrganisationId();

            final var organisationRecord = organisationRepository.findById(organisationId)
                    .orElseThrow(() -> new OrganisationNotFoundException(organisationId));

            final var schemaId = organisationRecord.getSchemaId();

            final var organisationSchemaRecord = organisationSchemaRepository.findById(schemaId)
                    .orElseThrow(() -> new OrganisationSchemaNotFoundException(schemaId));

            final var roles = organisationRoleService.findAllByRaidOrganisationId(record.getId());

            organisations.add(organisationFactory.create(
                    organisationRecord.getPid(),
                    organisationSchemaRecord.getUri(),
                    roles
            ));
        }

        return organisations;
    }

    public String findOrganisationUri(final Integer organisationId) {
        final var record = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new OrganisationNotFoundException(organisationId));
        return record.getPid();
    }

    public String findOrganisationSchemaUri(final Integer organisationId) {
        final var record = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new OrganisationNotFoundException(organisationId));

        final var schemaId = record.getSchemaId();

        final var schemaRecord = organisationSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new OrganisationSchemaNotFoundException(schemaId));

        return schemaRecord.getUri();
    }

    public void update(final List<Organisation> organisations, final String handle) {
        if (organisations == null || organisations.isEmpty()) {
            raidOrganisationRepository.deleteAllByHandle(handle);
            return;
        }

        // Create a set of new organisation IDs to keep
        final Set<Integer> newOrganisationIds = new HashSet<>();

        for (final var organisation : organisations) {
            final var organisationSchemaRecord = organisationSchemaRepository.findByUri(organisation.getSchemaUri())
                    .orElseThrow(() ->
                            new OrganisationSchemaNotFoundException(organisation.getSchemaUri()));

            final var organisationRecord = organisationRecordFactory.create(
                    organisation,
                    organisationSchemaRecord.getId()
            );
            final var savedOrganisation = organisationRepository.findOrCreate(organisationRecord);
            newOrganisationIds.add(savedOrganisation.getId());

            // Use upsert instead of delete + insert
            final var raidOrganisationRecord = raidOrganisationRecordFactory.create(
                    savedOrganisation.getId(),
                    handle
            );

            final var savedRaidOrganisationRecord = raidOrganisationRepository.upsert(
                    raidOrganisationRecord
            );
        }

        // Delete any old organisations that aren't in the new list
        raidOrganisationRepository.deleteByHandleAndNotInOrganisationIds(handle, newOrganisationIds);
    }
}
