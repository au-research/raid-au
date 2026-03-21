package au.org.raid.api.service;

import au.org.raid.api.exception.RelatedObjectNotFoundException;
import au.org.raid.api.exception.RelatedObjectSchemaNotFoundException;
import au.org.raid.api.exception.RelatedObjectTypeNotFoundException;
import au.org.raid.api.exception.RelatedObjectTypeSchemaNotFoundException;
import au.org.raid.api.factory.RelatedObjectFactory;
import au.org.raid.api.factory.record.RaidRelatedObjectRecordFactory;
import au.org.raid.api.factory.record.RelatedObjectRecordFactory;
import au.org.raid.api.repository.*;
import au.org.raid.idl.raidv2.model.RelatedObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class RelatedObjectService {
    private final RelatedObjectTypeSchemaRepository relatedObjectTypeSchemaRepository;
    private final RelatedObjectTypeRepository relatedObjectTypeRepository;
    private final RelatedObjectSchemaRepository relatedObjectSchemaRepository;
    private final RelatedObjectRepository relatedObjectRepository;
    private final RaidRelatedObjectRepository raidRelatedObjectRepository;
    private final RelatedObjectRecordFactory relatedObjectRecordFactory;
    private final RaidRelatedObjectRecordFactory raidRelatedObjectRecordFactory;
    private final RaidRelatedObjectCategoryService raidRelatedObjectCategoryService;
    private final RelatedObjectTypeService relatedObjectTypeService;
    private final RelatedObjectFactory relatedObjectFactory;

    public void create(final List<RelatedObject> relatedObjects, final String handle) {
        if (relatedObjects == null) {
            return;
        }

        for (final var relatedObject : relatedObjects) {
            final var relatedObjectSchemaRecord = relatedObjectSchemaRepository.findByUri(relatedObject.getSchemaUri())
                    .orElseThrow(() -> new RelatedObjectSchemaNotFoundException(relatedObject.getSchemaUri()));

            final var relatedObjectRecord =
                    relatedObjectRecordFactory.create(relatedObject.getId(), relatedObjectSchemaRecord.getId());

            final var savedRelatedObjectRecord = relatedObjectRepository.findOrCreate(relatedObjectRecord);

            final var relatedObjectTypeSchemaRecord =
                    relatedObjectTypeSchemaRepository.findByUri(relatedObject.getType().getSchemaUri())
                            .orElseThrow(() ->
                                    new RelatedObjectTypeSchemaNotFoundException(relatedObject.getSchemaUri()));

            final var relatedObjectTypeRecord = relatedObjectTypeRepository.findByUriAndSchemaId(
                    relatedObject.getType().getId(),
                    relatedObjectTypeSchemaRecord.getId()).orElseThrow(() ->
                        new RelatedObjectTypeNotFoundException(relatedObject.getId(), relatedObject.getSchemaUri()));

            final var raidRelatedObjectRecord = raidRelatedObjectRepository.create(raidRelatedObjectRecordFactory.create(
                    handle, savedRelatedObjectRecord.getId(), relatedObjectTypeRecord.getId()));

            for (final var category : relatedObject.getCategory()) {
                raidRelatedObjectCategoryService.create(category, raidRelatedObjectRecord.getId());
            }
        }
    }

    public List<RelatedObject> findAllByHandle(final String handle) {
        final var relatedObjects = new ArrayList<RelatedObject>();
        final var records = raidRelatedObjectRepository.findAllByHandle(handle);

        for (final var record : records) {
            final var relatedObjectId = record.getRelatedObjectId();

            final var relatedObjectRecord = relatedObjectRepository.findById(relatedObjectId)
                    .orElseThrow(() -> new RelatedObjectNotFoundException(relatedObjectId));

            final var schemaId = relatedObjectRecord.getSchemaId();

            final var relatedObjectSchemaRecord = relatedObjectSchemaRepository.findById(schemaId)
                    .orElseThrow(() -> new RelatedObjectSchemaNotFoundException(schemaId));

            final var type = relatedObjectTypeService.findById(record.getRelatedObjectTypeId());
            final var categories = raidRelatedObjectCategoryService.findAllByRaidRelatedObjectId(record.getId());

            relatedObjects.add(relatedObjectFactory.create(
                    relatedObjectRecord.getPid(),
                    relatedObjectSchemaRecord.getUri(),
                    type,
                    categories
            ));

        }
        return relatedObjects;
    }

    public void update(final List<RelatedObject> relatedObjects, final String handle) {
        raidRelatedObjectRepository.deleteAllByHandle(handle);
        create(relatedObjects, handle);
    }
}
