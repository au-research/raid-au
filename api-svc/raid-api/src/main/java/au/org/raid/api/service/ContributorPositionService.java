package au.org.raid.api.service;

import au.org.raid.api.exception.ContributorPositionSchemaNotFoundException;
import au.org.raid.api.factory.ContributorPositionFactory;
import au.org.raid.api.factory.record.RaidContributorPositionRecordFactory;
import au.org.raid.api.repository.ContributorPositionRepository;
import au.org.raid.api.repository.ContributorPositionSchemaRepository;
import au.org.raid.api.repository.RaidContributorPositionRepository;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import au.org.raid.api.exception.ContributorPositionNotFoundException;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ContributorPositionService {
    private final RaidContributorPositionRepository raidContributorPositionRepository;
    private final RaidContributorPositionRecordFactory raidContributorPositionRecordFactory;
    private final ContributorPositionFactory contributorPositionFactory;
    private final ContributorPositionRepository contributorPositionRepository;
    private final ContributorPositionSchemaRepository contributorPositionSchemaRepository;

    public List<ContributorPosition> findAllByRaidContributorId(final Integer raidContributorId) {
        final var contributorPositions = new ArrayList<ContributorPosition>();

        final var raidContributorPositions =
                raidContributorPositionRepository.findAllByRaidContributorId(raidContributorId);

        for (final var raidContributorPosition : raidContributorPositions) {
            final var contributorPositionId = raidContributorPosition.getContributorPositionId();

            final var contributorPositionRecord = contributorPositionRepository
                    .findById(contributorPositionId)
                    .orElseThrow(() -> new ContributorPositionNotFoundException(contributorPositionId));

            final var schemaId = contributorPositionRecord.getSchemaId();

            final var contributorPositionSchemaRecord = contributorPositionSchemaRepository.findById(schemaId)
                    .orElseThrow(() -> new ContributorPositionSchemaNotFoundException(schemaId));

            contributorPositions.add(contributorPositionFactory.create(
                    raidContributorPosition,
                    contributorPositionRecord.getUri(),
                    contributorPositionSchemaRecord.getUri())
            );
        }
        return contributorPositions;
    }

    public void create(final List<ContributorPosition> positions, final int raidContributorId) {
        if (positions == null) {
            return;
        }

        for (final var position : positions) {
            final var positionSchema = contributorPositionSchemaRepository.findByUri(position.getSchemaUri())
                    .orElseThrow(() -> new ContributorPositionSchemaNotFoundException(position.getSchemaUri()));

            final var contributorPosition = contributorPositionRepository.findByUriAndSchemaId(position.getId(), positionSchema.getId())
                    .orElseThrow(() ->
                            new ContributorPositionNotFoundException(position.getId(), position.getSchemaUri()));

            final var raidContributorPositionRecord = raidContributorPositionRecordFactory.create(
                    position, raidContributorId, contributorPosition.getId());

            raidContributorPositionRepository.create(raidContributorPositionRecord);
        }
    }
}
