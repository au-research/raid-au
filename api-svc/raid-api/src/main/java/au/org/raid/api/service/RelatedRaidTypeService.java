package au.org.raid.api.service;

import au.org.raid.api.exception.RelatedRaidTypeNotFoundException;
import au.org.raid.api.exception.RelatedRaidTypeSchemaNotFoundException;
import au.org.raid.api.factory.RelatedRaidTypeFactory;
import au.org.raid.api.repository.RelatedRaidTypeRepository;
import au.org.raid.api.repository.RelatedRaidTypeSchemaRepository;
import au.org.raid.idl.raidv2.model.RelatedRaidType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RelatedRaidTypeService {
    private final RelatedRaidTypeSchemaRepository relatedRaidTypeSchemaRepository;
    private final RelatedRaidTypeRepository relatedRaidTypeRepository;
    private final RelatedRaidTypeFactory relatedRaidTypeFactory;

    public RelatedRaidType findById(final Integer id) {
        final var record = relatedRaidTypeRepository.findById(id)
                .orElseThrow(() -> new RelatedRaidTypeNotFoundException(id));

        final var schemaId = record.getSchemaId();

        final var schemaRecord = relatedRaidTypeSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new RelatedRaidTypeSchemaNotFoundException(schemaId));

        return relatedRaidTypeFactory.create(record.getUri(), schemaRecord.getUri());
    }

    public Integer findId(final RelatedRaidType type) {
        final var relatedRaidTypeSchemaRecord = relatedRaidTypeSchemaRepository.findByUri(type.getSchemaUri())
                .orElseThrow(() -> new RelatedRaidTypeSchemaNotFoundException(type.getSchemaUri()));

        final var relatedRaidTypeRecord = relatedRaidTypeRepository.findByUriAndSchemaId(
                        type.getId(), relatedRaidTypeSchemaRecord.getId())
                .orElseThrow(() -> new RelatedRaidTypeNotFoundException(type.getId(), type.getSchemaUri()));

        return relatedRaidTypeRecord.getId();
    }
}
