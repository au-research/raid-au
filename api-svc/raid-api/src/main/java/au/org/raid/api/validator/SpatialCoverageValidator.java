package au.org.raid.api.validator;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.idl.raidv2.model.SpatialCoverage;
import au.org.raid.idl.raidv2.model.UnavailableResolver;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
@RequiredArgsConstructor
public class SpatialCoverageValidator {
    private final SpatialCoveragePlaceValidator placeValidator;
    private final Map<String, BiFunction<String, String, List<ValidationFailure>>> spatialCoverageUriValidatorMap;

    public ValidationResult validate(final List<SpatialCoverage> spatialCoverages) {
        final var failures = new ArrayList<ValidationFailure>();
        final var unavailable = new ArrayList<UnavailableResolver>();

        if (spatialCoverages == null) {
            return new ValidationResult(failures, unavailable);
        }

        IntStream.range(0, spatialCoverages.size())
                .forEach(i -> {
                    final var spatialCoverage = spatialCoverages.get(i);

                    if (isBlank(spatialCoverage.getId())) {
                        failures.add(new ValidationFailure()
                                .fieldId(String.format("spatialCoverage[%d].id", i))
                                .errorType(NOT_SET_TYPE)
                                .message(NOT_SET_MESSAGE));
                    }
                    if (spatialCoverage.getSchemaUri() == null) {
                        failures.add(new ValidationFailure()
                                .fieldId(String.format("spatialCoverage[%d].schemaUri", i))
                                .errorType(NOT_SET_TYPE)
                                .message(NOT_SET_MESSAGE));
                    } else if (spatialCoverageUriValidatorMap.containsKey(spatialCoverage.getSchemaUri().getValue())) {
                        final var uriValidatorFunction = spatialCoverageUriValidatorMap.get(spatialCoverage.getSchemaUri().getValue());

                        try {
                            failures.addAll(uriValidatorFunction.apply(spatialCoverage.getId(), "spatialCoverage[%d].id".formatted(i)));
                        } catch (ResolverUnavailableException e) {
                            // The resolver, not the spatialCoverage, is at fault (RAID-809).
                            // Collect and continue so one entry's resolver failure doesn't abort
                            // validation of the rest of the request.
                            unavailable.addAll(e.getUnavailableResolvers());
                        }
                    } else {
                        failures.add(new ValidationFailure()
                                .fieldId(String.format("spatialCoverage[%d].schemaUri", i))
                                .errorType(INVALID_VALUE_TYPE)
                                .message(INVALID_SCHEMA));
                    }
                    failures.addAll(placeValidator.validate(spatialCoverage.getPlace(), i));
                });

        return new ValidationResult(failures, unavailable);
    }
}
