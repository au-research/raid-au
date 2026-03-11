package au.org.raid.api.validator;

import au.org.raid.api.endpoint.message.ValidationMessage;
import au.org.raid.api.exception.ValidationFailureException;
import au.org.raid.api.service.raid.id.IdentifierHandle;
import au.org.raid.api.service.raid.id.IdentifierParser;
import au.org.raid.api.service.raid.id.IdentifierUrl;
import au.org.raid.api.util.Log;
import au.org.raid.idl.raidv2.model.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static au.org.raid.api.endpoint.message.ValidationMessage.HANDLE_DOES_NOT_MATCH_MESSAGE;
import static au.org.raid.api.endpoint.message.ValidationMessage.handlesDoNotMatch;
import static au.org.raid.api.util.Log.to;
import static au.org.raid.api.util.RestUtil.urlDecode;
import static au.org.raid.api.util.StringUtil.areDifferent;
import static au.org.raid.api.util.StringUtil.isBlank;
import static java.util.List.of;

@Component
public class ValidationService {
    private static final Log log = to(ValidationService.class);

    private final TitleValidator titleValidator;
    private final DescriptionValidator descriptionValidator;
    private final ContributorValidator contributorValidator;
    private final OrganisationValidator organisationValidator;
    private final AccessValidator accessValidator;
    private final SubjectValidator subjectValidator;
    private final IdentifierParser idParser;
    private final RelatedObjectValidator relatedObjectValidator;
    private final AlternateIdentifierValidator alternateIdentifierValidator;
    private final RelatedRaidValidator relatedRaidValidator;
    private final SpatialCoverageValidator spatialCoverageValidator;
    private final DateValidator dateValidator;
    private final Executor taskExecutor;

    public ValidationService(
            final TitleValidator titleValidator,
            final DescriptionValidator descriptionValidator,
            final ContributorValidator contributorValidator,
            final OrganisationValidator organisationValidator,
            final AccessValidator accessValidator,
            final SubjectValidator subjectValidator,
            final IdentifierParser idParser,
            final RelatedObjectValidator relatedObjectValidator,
            final AlternateIdentifierValidator alternateIdentifierValidator,
            final RelatedRaidValidator relatedRaidValidator,
            final SpatialCoverageValidator spatialCoverageValidator,
            final DateValidator dateValidator,
            @Qualifier("applicationTaskExecutor") final Executor taskExecutor
    ) {
        this.titleValidator = titleValidator;
        this.descriptionValidator = descriptionValidator;
        this.contributorValidator = contributorValidator;
        this.organisationValidator = organisationValidator;
        this.accessValidator = accessValidator;
        this.subjectValidator = subjectValidator;
        this.idParser = idParser;
        this.relatedObjectValidator = relatedObjectValidator;
        this.alternateIdentifierValidator = alternateIdentifierValidator;
        this.relatedRaidValidator = relatedRaidValidator;
        this.spatialCoverageValidator = spatialCoverageValidator;
        this.dateValidator = dateValidator;
        this.taskExecutor = taskExecutor;
    }

    private List<ValidationFailure> validateUpdateHandle(final String decodedHandleFromPath, final Id id) {
        final var failures = new ArrayList<ValidationFailure>();

        IdentifierUrl updateId = null;
        try {
            updateId = idParser.parseUrlWithException(id.getId());
        } catch (ValidationFailureException e) {
            failures.addAll(e.getFailures());
        }

        IdentifierHandle pathHandle = null;
        try {
            pathHandle = idParser.parseHandleWithException(decodedHandleFromPath);
        } catch (ValidationFailureException e) {
            failures.addAll(e.getFailures());
        }

        if (updateId != null && pathHandle != null) {
            if (areDifferent(pathHandle.format(), updateId.handle().format())) {
                log.with("pathHandle", pathHandle.format()).
                        with("updateId", updateId.handle().format()).
                        error(HANDLE_DOES_NOT_MATCH_MESSAGE);
                failures.add(handlesDoNotMatch());
            }
        }

        return failures;
    }

    public List<ValidationFailure> validateAlternateUrls(
            List<AlternateUrl> urls
    ) {
        if (urls == null) {
            return Collections.emptyList();
        }

        var failures = new ArrayList<ValidationFailure>();

        for (int i = 0; i < urls.size(); i++) {
            var iUrl = urls.get(i);

            if (isBlank(iUrl.getUrl())) {
                failures.add(ValidationMessage.alternateUrlNotSet(i));
            }

      /* not sure yet if we want to be doing any further validation of this
      - only https, or only http/https?
      - must be a formal URI?
      - url must exist (ping if it returns a 200 response?) */
        }
        return failures;
    }

    public List<ValidationFailure> validateForCreate(final RaidCreateRequest request) {
        if (request == null) {
            return of(ValidationMessage.METADATA_NOT_SET);
        }

        // Run in-memory validators synchronously — they are essentially free.
        var failures = new ArrayList<ValidationFailure>();
        failures.addAll(dateValidator.validate(request.getDate()));
        failures.addAll(accessValidator.validate(request.getAccess()));
        failures.addAll(titleValidator.validate(request.getTitle()));
        failures.addAll(descriptionValidator.validate(request.getDescription()));
        failures.addAll(validateAlternateUrls(request.getAlternateUrl()));
        failures.addAll(subjectValidator.validate(request.getSubject()));
        failures.addAll(relatedRaidValidator.validate(request.getRelatedRaid()));
        failures.addAll(alternateIdentifierValidator.validateAlternateIdentifier(request.getAlternateIdentifier()));

        failures.addAll(runIoBoundValidators(
                () -> contributorValidator.validate(request.getContributor()),
                () -> organisationValidator.validate(request.getOrganisation()),
                () -> relatedObjectValidator.validateRelatedObjects(request.getRelatedObject()),
                () -> spatialCoverageValidator.validate(request.getSpatialCoverage())
        ));

        return failures;
    }

    public List<ValidationFailure> validateForUpdate(final String handle, final RaidUpdateRequest request) {
        if (request == null) {
            return of(ValidationMessage.METADATA_NOT_SET);
        }

        String decodedHandle = urlDecode(handle);

        // Run in-memory validators synchronously — they are essentially free.
        final var failures = new ArrayList<>(validateUpdateHandle(decodedHandle, request.getIdentifier()));
        failures.addAll(dateValidator.validate(request.getDate()));
        failures.addAll(accessValidator.validate(request.getAccess()));
        failures.addAll(titleValidator.validate(request.getTitle()));
        failures.addAll(descriptionValidator.validate(request.getDescription()));
        failures.addAll(validateAlternateUrls(request.getAlternateUrl()));
        failures.addAll(subjectValidator.validate(request.getSubject()));
        failures.addAll(relatedRaidValidator.validate(request.getRelatedRaid()));
        failures.addAll(alternateIdentifierValidator.validateAlternateIdentifier(request.getAlternateIdentifier()));

        failures.addAll(runIoBoundValidators(
                () -> contributorValidator.validate(request.getContributor()),
                () -> organisationValidator.validate(request.getOrganisation()),
                () -> relatedObjectValidator.validateRelatedObjects(request.getRelatedObject()),
                () -> spatialCoverageValidator.validate(request.getSpatialCoverage())
        ));

        return failures;
    }

    public List<ValidationFailure> validateForPatch(final RaidPatchRequest request) {
        return new ArrayList<>(contributorValidator.validateForPatch(request.getContributor()));
    }

    /**
     * Submits each supplier as an async task, waits for all of them to finish
     * via allOf(), then merges their results. Every validator runs to
     * completion before any result is inspected.
     *
     * If a validator throws a RuntimeException it is re-thrown unwrapped —
     * the CompletionException wrapper added by CompletableFuture is removed so
     * callers see the original exception type.
     */
    @SafeVarargs
    private List<ValidationFailure> runIoBoundValidators(
            Supplier<List<ValidationFailure>>... validators) {

        @SuppressWarnings("unchecked")
        CompletableFuture<List<ValidationFailure>>[] futures =
                new CompletableFuture[validators.length];

        for (int i = 0; i < validators.length; i++) {
            final Supplier<List<ValidationFailure>> validator = validators[i];
            futures[i] = CompletableFuture.supplyAsync(validator, taskExecutor);
        }

        // Wait for all futures before inspecting any result. allOf().join()
        // throws a CompletionException if any future failed, but we discard
        // that here and re-inspect each future individually so we can unwrap.
        try {
            CompletableFuture.allOf(futures).join();
        } catch (CompletionException ignored) {
            // At least one future failed. Fall through to per-future join()
            // calls below which will unwrap and re-throw the original exception.
        }

        var results = new ArrayList<ValidationFailure>();
        for (CompletableFuture<List<ValidationFailure>> future : futures) {
            // join() on an individually failed future throws CompletionException.
            // Unwrap it so callers receive the original RuntimeException type.
            try {
                results.addAll(future.join());
            } catch (CompletionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException(cause);
            }
        }
        return results;
    }
}
