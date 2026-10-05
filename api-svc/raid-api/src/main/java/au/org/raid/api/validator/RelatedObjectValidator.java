package au.org.raid.api.validator;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.api.repository.RelatedObjectTypeRepository;
import au.org.raid.api.service.doi.DoiService;
import au.org.raid.api.service.handle.HandleService;
import au.org.raid.api.service.rrid.RridService;
import au.org.raid.api.service.webarchive.WebArchiveService;
import au.org.raid.idl.raidv2.model.RelatedObject;
import au.org.raid.idl.raidv2.model.UnavailableResolver;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static au.org.raid.api.endpoint.message.ValidationMessage.NOT_SET_MESSAGE;
import static au.org.raid.api.endpoint.message.ValidationMessage.NOT_SET_TYPE;
import static au.org.raid.api.util.StringUtil.isBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RelatedObjectValidator {
    private static final Logger log = LoggerFactory.getLogger(RelatedObjectValidator.class);
    private static final String RELATED_OBJECT_TYPE_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/related-object-type/";

    private static final String RELATED_OBJECT_TYPE_URL_PREFIX =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/related-object-type/";

    private static final List<String> VALID_CATEGORY_TYPES =
            List.of("Input", "Output", "Internal process document or artefact");

    private static final String DOI_SCHEMA_URI = "https://doi.org/";
    private static final String WEB_ARCHIVE_SCHEMA_URI = "https://web.archive.org/";
    private static final String HANDLE_SCHEMA_URI = "https://hdl.handle.net/";
    private static final String RRID_SCHEMA_URI = "https://scicrunch.org/resolver/";

    private final RelatedObjectTypeValidator typeValidationService;
    private final RelatedObjectCategoryValidator categoryValidationService;
    private final Map<String, UriValidator> relatedObjectSchemaUriValidatorMap;
    private final RelatedObjectSuccessCache successCache;

    public RelatedObjectValidator(final RelatedObjectTypeRepository relatedObjectTypeRepository, final DoiService doiService, final HandleService handleService, final RridService rridService, final WebArchiveService webArchiveService, final RelatedObjectTypeValidator typeValidationService, final RelatedObjectCategoryValidator categoryValidationService, final RelatedObjectSuccessCache successCache) {
        this.typeValidationService = typeValidationService;
        this.categoryValidationService = categoryValidationService;
        this.successCache = successCache;

        // Built here (rather than as a Spring @Bean, cf. ExternalPidService#spatialCoverageUriValidatorMap)
        // because keeping the whole dispatch map private to this validator avoids splitting a
        // single-owner concern across two classes.
        final var map = new LinkedHashMap<String, UriValidator>();
        map.put(DOI_SCHEMA_URI, doiService);
        map.put(HANDLE_SCHEMA_URI, handleService);
        map.put(RRID_SCHEMA_URI, rridService);
        map.put(WEB_ARCHIVE_SCHEMA_URI, webArchiveService);
        this.relatedObjectSchemaUriValidatorMap = Collections.unmodifiableMap(map);
    }

    /**
     * Validates every related object, calling each resolver for every one.
     */
    public ValidationResult validateRelatedObjects(final List<RelatedObject> relatedObjects) {
        return validateRelatedObjects(relatedObjects, Set.of());
    }

    /**
     * Validates every related object. The local checks (format, the Web Archive capture year,
     * type, category and the schemaUri allow-list) run for every item. The resolver call is
     * skipped for an item whose exact {@code (schemaUri, id)} is in {@code storedKeys} (already
     * on the version being edited) or in the success cache (confirmed by an earlier check).
     * Failure field ids always use the item's index in {@code relatedObjects} (RAID-935).
     *
     * @param storedKeys the related objects of the stored version the client edited; empty on
     *                   create, or when that version could not be read
     */
    public ValidationResult validateRelatedObjects(final List<RelatedObject> relatedObjects, final Set<RelatedObjectKey> storedKeys) {
        final var failures = new ArrayList<ValidationFailure>();
        final var unavailable = new ArrayList<UnavailableResolver>();

        if (relatedObjects == null) {
            return new ValidationResult(failures, unavailable);
        }

        IntStream.range(0, relatedObjects.size())
                .forEach(index -> {
                    final var relatedObject = relatedObjects.get(index);

                    log.debug("Validating relatedObject: {}", relatedObject);

                    final var schemaUriValue = relatedObject.getSchemaUri() == null ? null : relatedObject.getSchemaUri().getValue();

                    if (isBlank(relatedObject.getId())) {
                        failures.add(new ValidationFailure()
                                .fieldId(String.format("relatedObject[%d].id", index))
                                .errorType(NOT_SET_TYPE)
                                .message(NOT_SET_MESSAGE));
                    } else if (schemaUriValue != null && relatedObjectSchemaUriValidatorMap.containsKey(schemaUriValue)) {
                        try {
                            failures.addAll(validateItem(
                                    relatedObjectSchemaUriValidatorMap.get(schemaUriValue),
                                    new RelatedObjectKey(schemaUriValue, relatedObject.getId()),
                                    String.format("relatedObject[%d].id", index),
                                    storedKeys == null ? Set.of() : storedKeys));
                        } catch (ResolverUnavailableException e) {
                            // The resolver, not the relatedObject, is at fault (RAID-809). Collect
                            // and continue so one relatedObject's resolver failure doesn't abort
                            // validation of the rest of the request.
                            unavailable.addAll(e.getUnavailableResolvers());
                        }
                    }

                    log.debug("relatedObject.schemaUri = {}", relatedObject.getSchemaUri());

                    if (schemaUriValue == null) {
                        failures.add(new ValidationFailure()
                                .fieldId(String.format("relatedObject[%d].schemaUri", index))
                                .errorType(NOT_SET_TYPE)
                                .message(NOT_SET_MESSAGE));
                    } else if (!relatedObjectSchemaUriValidatorMap.containsKey(schemaUriValue)) {
                        failures.add(new ValidationFailure()
                                .fieldId(String.format("relatedObject[%d].schemaUri", index))
                                .errorType("invalid")
                                .message(String.format("Only %s is supported.", relatedObjectSchemaUriValidatorMap.keySet())));
                    }

                    failures.addAll(typeValidationService.validate(relatedObject.getType(), index));
                    failures.addAll(categoryValidationService.validate(relatedObject.getCategory(), index));
                });

        return new ValidationResult(failures, unavailable);
    }

    /**
     * A known-good item (stored on the edited version, or confirmed by an earlier check) runs
     * the local checks only. Anything else gets the full check, and a clean result is
     * remembered. A failure or {@link ResolverUnavailableException} is never remembered.
     */
    private List<ValidationFailure> validateItem(
            final UriValidator validator,
            final RelatedObjectKey key,
            final String fieldId,
            final Set<RelatedObjectKey> storedKeys) {

        final String reason;
        if (storedKeys.contains(key)) {
            reason = "stored";
        } else if (successCache.contains(key)) {
            reason = "cached";
        } else {
            reason = null;
        }

        if (reason != null) {
            log.debug("Skipping resolver check for {} ({}): already {}", fieldId, key.id(), reason);
            return validator.validateLocally(key.id(), fieldId);
        }

        final var failures = validator.validate(key.id(), fieldId);
        if (failures.isEmpty()) {
            successCache.recordSuccess(key);
        }
        return failures;
    }
}
