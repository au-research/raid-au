package au.org.raid.api.validator;

import au.org.raid.api.repository.RelatedObjectTypeRepository;
import au.org.raid.api.service.doi.DoiService;
import au.org.raid.idl.raidv2.model.RelatedObject;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
    private static final List<String> RELATED_OBJECT_SCHEMA_URI =
            List.of(DOI_SCHEMA_URI, WEB_ARCHIVE_SCHEMA_URI);
    private static final Pattern WEB_ARCHIVE_URL_PATTERN =
            Pattern.compile("https://web\\.archive\\.org/web/\\d{14}/https?://.+");

    private final DoiService doiService;
    private final RelatedObjectTypeValidator typeValidationService;
    private final RelatedObjectCategoryValidator categoryValidationService;

    public RelatedObjectValidator(final RelatedObjectTypeRepository relatedObjectTypeRepository, final DoiService doiService, final RelatedObjectTypeValidator typeValidationService, final RelatedObjectCategoryValidator categoryValidationService) {
        this.doiService = doiService;
        this.typeValidationService = typeValidationService;
        this.categoryValidationService = categoryValidationService;
    }

    public List<ValidationFailure> validateRelatedObjects(final List<RelatedObject> relatedObjects) {
        final var failures = new ArrayList<ValidationFailure>();

        if (relatedObjects == null) {
            return failures;
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
                    }   else if (DOI_SCHEMA_URI.equals(schemaUriValue)) {
                        failures.addAll(
                                doiService.validate(relatedObject.getId(), String.format("relatedObject[%d].id", index))
                        );
                    } else if (WEB_ARCHIVE_SCHEMA_URI.equals(schemaUriValue)) {
                        // validate web archive URL format
                        if (!WEB_ARCHIVE_URL_PATTERN.matcher(relatedObject.getId()).matches()) {
                            failures.add(new ValidationFailure()
                                    .fieldId(String.format("relatedObject[%d].id", index))
                                    .errorType("invalid")
                                    .message("Must be a valid Web Archive URL (e.g. https://web.archive.org/web/20220101000000/https://example.com)"));
                        }
                    }

                    log.debug("relatedObject.schemaUri = {}", relatedObject.getSchemaUri());

                    if (schemaUriValue == null) {
                        failures.add(new ValidationFailure()
                                .fieldId(String.format("relatedObject[%d].schemaUri", index))
                                .errorType(NOT_SET_TYPE)
                                .message(NOT_SET_MESSAGE));
                    } else if (!RELATED_OBJECT_SCHEMA_URI.contains(schemaUriValue)) {
                        failures.add(new ValidationFailure()
                                .fieldId(String.format("relatedObject[%d].schemaUri", index))
                                .errorType("invalid")
                                .message(String.format("Only %s is supported.", RELATED_OBJECT_SCHEMA_URI)));
                    }

                    failures.addAll(typeValidationService.validate(relatedObject.getType(), index));
                    failures.addAll(categoryValidationService.validate(relatedObject.getCategory(), index));
                });

        return failures;
    }
}
