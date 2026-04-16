package au.org.raid.api.service;

import au.org.raid.api.exception.RelatedObjectCategoryNotFoundException;
import au.org.raid.api.exception.RelatedObjectCategorySchemaNotFoundException;
import au.org.raid.api.factory.RelatedObjectCategoryFactory;
import au.org.raid.api.factory.record.RaidRelatedObjectCategoryRecordFactory;
import au.org.raid.api.repository.RaidRelatedObjectCategoryRepository;
import au.org.raid.api.repository.RelatedObjectCategorySchemaRepository;
import au.org.raid.api.repository.RelatedObjectCategoryRepository;
import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class RaidRelatedObjectCategoryService {
    private final RelatedObjectCategorySchemaRepository relatedObjectCategorySchemaRepository;
    private final RelatedObjectCategoryRepository relatedObjectCategoryRepository;
    private final RaidRelatedObjectCategoryRepository raidRelatedObjectCategoryRepository;
    private final RaidRelatedObjectCategoryRecordFactory raidRelatedObjectCategoryRecordFactory;
    private final RelatedObjectCategoryFactory relatedObjectCategoryFactory;

    public void create(final RelatedObjectCategory category, final Integer raidRelatedObjectId) {
        final var schemaRecord = relatedObjectCategorySchemaRepository.findByUri(category.getSchemaUri())
                .orElseThrow(() -> new RelatedObjectCategorySchemaNotFoundException(category.getSchemaUri()));

        final var categoryRecord = relatedObjectCategoryRepository
                .findByUriAndSchemaId(category.getId(), schemaRecord.getId())
                .orElseThrow(() -> new RelatedObjectCategoryNotFoundException(category.getId(), category.getSchemaUri()));

        final var raidRelatedObjectCategoryRecord =
                raidRelatedObjectCategoryRecordFactory.create(raidRelatedObjectId, categoryRecord.getId());

        raidRelatedObjectCategoryRepository.create(raidRelatedObjectCategoryRecord);
    }

    public List<RelatedObjectCategory> findAllByRaidRelatedObjectId(final Integer raidRelatedObjectId) {
        final var categories = new ArrayList<RelatedObjectCategory>();

        final var records = raidRelatedObjectCategoryRepository.findAllByRaidRelatedObjectId(raidRelatedObjectId);

        for (final var record : records) {
            final var categoryId = record.getRelatedObjectCategoryId();
            final var categoryRecord = relatedObjectCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RelatedObjectCategoryNotFoundException(categoryId));

            final var schemaId = categoryRecord.getSchemaId();

            final var categorySchemaRecord = relatedObjectCategorySchemaRepository.findById(schemaId)
                    .orElseThrow(() -> new RelatedObjectCategorySchemaNotFoundException(schemaId));

            categories.add(relatedObjectCategoryFactory.create(categoryRecord.getUri(), categorySchemaRecord.getUri()));
        }

        return categories;
    }
}
