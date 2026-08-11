package au.org.raid.api.validator;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.api.repository.RelatedObjectTypeRepository;
import au.org.raid.api.service.doi.DoiService;
import au.org.raid.api.service.handle.HandleService;
import au.org.raid.api.service.rrid.RridService;
import au.org.raid.idl.raidv2.model.RelatedObject;
import au.org.raid.idl.raidv2.model.UnavailableResolver;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
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
    private static final Pattern WEB_ARCHIVE_URL_PATTERN =
            Pattern.compile("https://web\\.archive\\.org/web/\\d{14}/https?://.+");

    private final RelatedObjectTypeValidator typeValidationService;
    private final RelatedObjectCategoryValidator categoryValidationService;
    private final Map<String, BiFunction<String, String, List<ValidationFailure>>> relatedObjectSchemaUriValidatorMap;

    public RelatedObjectValidator(final RelatedObjectTypeRepository relatedObjectTypeRepository, final DoiService doiService, final HandleService handleService, final RridService rridService, final RelatedObjectTypeValidator typeValidationService, final RelatedObjectCategoryValidator categoryValidationService) {
        this.typeValidationService = typeValidationService;
        this.categoryValidationService = categoryValidationService;

        // Built here (rather than as a Spring @Bean, cf. ExternalPidService#spatialCoverageUriValidatorMap)
        // because the web-archive entry is a local regex check with no injectable collaborator - keeping the
        // whole dispatch map private to this validator avoids splitting a single-owner concern across two classes.
        final var map = new LinkedHashMap<String, BiFunction<String, String, List<ValidationFailure>>>();
        map.put(DOI_SCHEMA_URI, doiService::validate);
        map.put(HANDLE_SCHEMA_URI, handleService::validate);
        map.put(RRID_SCHEMA_URI, rridService::validate);
        map.put(WEB_ARCHIVE_SCHEMA_URI, (id, fieldId) -> {
            if (WEB_ARCHIVE_URL_PATTERN.matcher(id).matches()) {
                return List.of();
            }

            return List.of(new ValidationFailure()
                    .fieldId(fieldId)
                    .errorType("invalid")
                    .message("Must be a valid Web Archive URL (e.g. https://web.archive.org/web/20220101000000/https://example.com)"));
        });
        this.relatedObjectSchemaUriValidatorMap = Collections.unmodifiableMap(map);
    }

    public ValidationResult validateRelatedObjects(final List<RelatedObject> relatedObjects) {
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
                            failures.addAll(
                                    relatedObjectSchemaUriValidatorMap.get(schemaUriValue)
                                            .apply(relatedObject.getId(), String.format("relatedObject[%d].id", index))
                            );
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
}
