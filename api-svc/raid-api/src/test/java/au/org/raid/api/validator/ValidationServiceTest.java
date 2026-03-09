package au.org.raid.api.validator;

import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.Executor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    // Use a direct (synchronous) executor so tests are deterministic and fast.
    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    @Mock
    private TitleValidator titleValidator;
    @Mock
    private DescriptionValidator descriptionValidator;
    @Mock
    private ContributorValidator contributorValidator;
    @Mock
    private OrganisationValidator organisationValidator;
    @Mock
    private AccessValidator accessValidator;
    @Mock
    private SubjectValidator subjectValidator;
    @Mock
    private au.org.raid.api.service.raid.id.IdentifierParser idParser;
    @Mock
    private RelatedObjectValidator relatedObjectValidator;
    @Mock
    private AlternateIdentifierValidator alternateIdentifierValidator;
    @Mock
    private RelatedRaidValidator relatedRaidValidator;
    @Mock
    private SpatialCoverageValidator spatialCoverageValidator;
    @Mock
    private DateValidator dateValidator;

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService(
                titleValidator,
                descriptionValidator,
                contributorValidator,
                organisationValidator,
                accessValidator,
                subjectValidator,
                idParser,
                relatedObjectValidator,
                alternateIdentifierValidator,
                relatedRaidValidator,
                spatialCoverageValidator,
                dateValidator,
                DIRECT_EXECUTOR
        );
    }

    // -----------------------------------------------------------------------
    // validateForCreate
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("validateForCreate returns METADATA_NOT_SET when request is null")
    void validateForCreate_returnsMetadataNotSet_whenRequestIsNull() {
        var failures = validationService.validateForCreate(null);

        assertThat(failures, hasSize(1));
        assertThat(failures.get(0).getFieldId(), equalTo("metadata"));
    }

    @Test
    @DisplayName("validateForCreate returns no failures when all validators pass")
    void validateForCreate_returnsNoFailures_whenAllValidatorsPass() {
        stubAllValidatorsEmpty();

        var failures = validationService.validateForCreate(new RaidCreateRequest());

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("validateForCreate collects failures from all synchronous in-memory validators")
    void validateForCreate_collectsFailuresFromSynchronousValidators() {
        stubAllValidatorsEmpty();

        var titleFailure = new ValidationFailure().message("bad title");
        var accessFailure = new ValidationFailure().message("bad access");
        when(titleValidator.validate(any())).thenReturn(List.of(titleFailure));
        when(accessValidator.validate(any())).thenReturn(List.of(accessFailure));

        var failures = validationService.validateForCreate(new RaidCreateRequest());

        assertThat(failures, hasItems(titleFailure, accessFailure));
    }

    @Test
    @DisplayName("validateForCreate collects failures from all I/O-bound validators")
    void validateForCreate_collectsFailuresFromIoBoundValidators() {
        stubAllValidatorsEmpty();

        var contributorFailure = new ValidationFailure().message("bad contributor");
        var orgFailure = new ValidationFailure().message("bad org");
        var relatedObjectFailure = new ValidationFailure().message("bad related object");
        var spatialFailure = new ValidationFailure().message("bad spatial");

        when(contributorValidator.validate(any())).thenReturn(List.of(contributorFailure));
        when(organisationValidator.validate(any())).thenReturn(List.of(orgFailure));
        when(relatedObjectValidator.validateRelatedObjects(any())).thenReturn(List.of(relatedObjectFailure));
        when(spatialCoverageValidator.validate(any())).thenReturn(List.of(spatialFailure));

        var failures = validationService.validateForCreate(new RaidCreateRequest());

        assertThat(failures, hasItems(contributorFailure, orgFailure, relatedObjectFailure, spatialFailure));
    }

    @Test
    @DisplayName("validateForCreate collects failures from all validators together")
    void validateForCreate_collectsFailuresFromAllValidators() {
        var dateFailure = new ValidationFailure().message("bad date");
        var titleFailure = new ValidationFailure().message("bad title");
        var contributorFailure = new ValidationFailure().message("bad contributor");
        var orgFailure = new ValidationFailure().message("bad org");
        var relatedObjectFailure = new ValidationFailure().message("bad related object");
        var spatialFailure = new ValidationFailure().message("bad spatial");

        when(dateValidator.validate(any())).thenReturn(List.of(dateFailure));
        when(titleValidator.validate(any())).thenReturn(List.of(titleFailure));
        when(accessValidator.validate(any())).thenReturn(List.of());
        when(descriptionValidator.validate(any())).thenReturn(List.of());
        when(subjectValidator.validate(any())).thenReturn(List.of());
        when(relatedRaidValidator.validate(any())).thenReturn(List.of());
        when(alternateIdentifierValidator.validateAlternateIdentifier(any())).thenReturn(List.of());
        when(contributorValidator.validate(any())).thenReturn(List.of(contributorFailure));
        when(organisationValidator.validate(any())).thenReturn(List.of(orgFailure));
        when(relatedObjectValidator.validateRelatedObjects(any())).thenReturn(List.of(relatedObjectFailure));
        when(spatialCoverageValidator.validate(any())).thenReturn(List.of(spatialFailure));

        var failures = validationService.validateForCreate(new RaidCreateRequest());

        assertThat(failures, hasItems(
                dateFailure, titleFailure, contributorFailure, orgFailure,
                relatedObjectFailure, spatialFailure));
    }

    @Test
    @DisplayName("validateForCreate invokes all 4 I/O-bound validators exactly once")
    void validateForCreate_invokesEachIoBoundValidatorOnce() {
        stubAllValidatorsEmpty();

        validationService.validateForCreate(new RaidCreateRequest());

        verify(contributorValidator, times(1)).validate(any());
        verify(organisationValidator, times(1)).validate(any());
        verify(relatedObjectValidator, times(1)).validateRelatedObjects(any());
        verify(spatialCoverageValidator, times(1)).validate(any());
    }

    @Test
    @DisplayName("validateForCreate propagates the original RuntimeException when an I/O-bound validator throws")
    void validateForCreate_propagatesOriginalException_whenIoBoundValidatorThrows() {
        stubAllValidatorsEmpty();

        var cause = new RuntimeException("ROR service unavailable");
        when(organisationValidator.validate(any())).thenThrow(cause);

        var thrown = assertThrows(RuntimeException.class,
                () -> validationService.validateForCreate(new RaidCreateRequest()));

        assertThat(thrown, equalTo(cause));
    }

    // -----------------------------------------------------------------------
    // validateForUpdate
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("validateForUpdate returns METADATA_NOT_SET when request is null")
    void validateForUpdate_returnsMetadataNotSet_whenRequestIsNull() {
        var failures = validationService.validateForUpdate("10.25.1/abc123", null);

        assertThat(failures, hasSize(1));
        assertThat(failures.get(0).getFieldId(), equalTo("metadata"));
    }

    @Test
    @DisplayName("validateForUpdate returns no failures when all validators pass")
    void validateForUpdate_returnsNoFailures_whenAllValidatorsPass() throws Exception {
        stubAllValidatorsEmpty();

        var handle = "10.25.1/abc123";
        var id = new Id().id("https://raid.org.au/" + handle);
        var request = new RaidUpdateRequest().identifier(id);

        var parsedUrl = mock(au.org.raid.api.service.raid.id.IdentifierUrl.class);
        var parsedHandle = mock(au.org.raid.api.service.raid.id.IdentifierHandle.class);
        when(idParser.parseUrlWithException(any())).thenReturn(parsedUrl);
        when(idParser.parseHandleWithException(any())).thenReturn(parsedHandle);
        when(parsedHandle.format()).thenReturn(handle);
        var parsedHandleInUrl = mock(au.org.raid.api.service.raid.id.IdentifierHandle.class);
        when(parsedUrl.handle()).thenReturn(parsedHandleInUrl);
        when(parsedHandleInUrl.format()).thenReturn(handle);

        var failures = validationService.validateForUpdate(handle, request);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("validateForUpdate collects failures from all I/O-bound validators")
    void validateForUpdate_collectsFailuresFromIoBoundValidators() throws Exception {
        stubAllValidatorsEmpty();

        var handle = "10.25.1/abc123";
        var id = new Id().id("https://raid.org.au/" + handle);
        var request = new RaidUpdateRequest().identifier(id);

        var parsedUrl = mock(au.org.raid.api.service.raid.id.IdentifierUrl.class);
        var parsedHandle = mock(au.org.raid.api.service.raid.id.IdentifierHandle.class);
        when(idParser.parseUrlWithException(any())).thenReturn(parsedUrl);
        when(idParser.parseHandleWithException(any())).thenReturn(parsedHandle);
        when(parsedHandle.format()).thenReturn(handle);
        var parsedHandleInUrl = mock(au.org.raid.api.service.raid.id.IdentifierHandle.class);
        when(parsedUrl.handle()).thenReturn(parsedHandleInUrl);
        when(parsedHandleInUrl.format()).thenReturn(handle);

        var contributorFailure = new ValidationFailure().message("bad contributor");
        var orgFailure = new ValidationFailure().message("bad org");
        var relatedObjectFailure = new ValidationFailure().message("bad related object");
        var spatialFailure = new ValidationFailure().message("bad spatial");

        when(contributorValidator.validate(any())).thenReturn(List.of(contributorFailure));
        when(organisationValidator.validate(any())).thenReturn(List.of(orgFailure));
        when(relatedObjectValidator.validateRelatedObjects(any())).thenReturn(List.of(relatedObjectFailure));
        when(spatialCoverageValidator.validate(any())).thenReturn(List.of(spatialFailure));

        var failures = validationService.validateForUpdate(handle, request);

        assertThat(failures, hasItems(contributorFailure, orgFailure, relatedObjectFailure, spatialFailure));
    }

    @Test
    @DisplayName("validateForUpdate invokes all 4 I/O-bound validators exactly once")
    void validateForUpdate_invokesEachIoBoundValidatorOnce() throws Exception {
        stubAllValidatorsEmpty();

        var handle = "10.25.1/abc123";
        var id = new Id().id("https://raid.org.au/" + handle);
        var request = new RaidUpdateRequest().identifier(id);

        var parsedUrl = mock(au.org.raid.api.service.raid.id.IdentifierUrl.class);
        var parsedHandle = mock(au.org.raid.api.service.raid.id.IdentifierHandle.class);
        when(idParser.parseUrlWithException(any())).thenReturn(parsedUrl);
        when(idParser.parseHandleWithException(any())).thenReturn(parsedHandle);
        when(parsedHandle.format()).thenReturn(handle);
        var parsedHandleInUrl = mock(au.org.raid.api.service.raid.id.IdentifierHandle.class);
        when(parsedUrl.handle()).thenReturn(parsedHandleInUrl);
        when(parsedHandleInUrl.format()).thenReturn(handle);

        validationService.validateForUpdate(handle, request);

        verify(contributorValidator, times(1)).validate(any());
        verify(organisationValidator, times(1)).validate(any());
        verify(relatedObjectValidator, times(1)).validateRelatedObjects(any());
        verify(spatialCoverageValidator, times(1)).validate(any());
    }

    @Test
    @DisplayName("validateForUpdate propagates the original RuntimeException when an I/O-bound validator throws")
    void validateForUpdate_propagatesOriginalException_whenIoBoundValidatorThrows() throws Exception {
        stubAllValidatorsEmpty();

        var handle = "10.25.1/abc123";
        var id = new Id().id("https://raid.org.au/" + handle);
        var request = new RaidUpdateRequest().identifier(id);

        var parsedUrl = mock(au.org.raid.api.service.raid.id.IdentifierUrl.class);
        var parsedHandle = mock(au.org.raid.api.service.raid.id.IdentifierHandle.class);
        when(idParser.parseUrlWithException(any())).thenReturn(parsedUrl);
        when(idParser.parseHandleWithException(any())).thenReturn(parsedHandle);
        when(parsedHandle.format()).thenReturn(handle);
        var parsedHandleInUrl = mock(au.org.raid.api.service.raid.id.IdentifierHandle.class);
        when(parsedUrl.handle()).thenReturn(parsedHandleInUrl);
        when(parsedHandleInUrl.format()).thenReturn(handle);

        var cause = new RuntimeException("ORCID service unavailable");
        when(contributorValidator.validate(any())).thenThrow(cause);

        var thrown = assertThrows(RuntimeException.class,
                () -> validationService.validateForUpdate(handle, request));

        assertThat(thrown, equalTo(cause));
    }

    // -----------------------------------------------------------------------
    // validateAlternateUrls
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("validateAlternateUrls returns empty when urls list is null")
    void validateAlternateUrls_returnsEmpty_whenNull() {
        var failures = validationService.validateAlternateUrls(null);
        assertThat(failures, empty());
    }

    @Test
    @DisplayName("validateAlternateUrls returns failure when url is blank")
    void validateAlternateUrls_returnsFailure_whenUrlIsBlank() {
        var url = new AlternateUrl().url("");
        var failures = validationService.validateAlternateUrls(List.of(url));
        assertThat(failures, hasSize(1));
    }

    @Test
    @DisplayName("validateAlternateUrls returns no failures when url is set")
    void validateAlternateUrls_returnsNoFailures_whenUrlIsSet() {
        var url = new AlternateUrl().url("https://example.com");
        var failures = validationService.validateAlternateUrls(List.of(url));
        assertThat(failures, empty());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void stubAllValidatorsEmpty() {
        when(dateValidator.validate(any())).thenReturn(List.of());
        when(accessValidator.validate(any())).thenReturn(List.of());
        when(titleValidator.validate(any())).thenReturn(List.of());
        when(descriptionValidator.validate(any())).thenReturn(List.of());
        when(subjectValidator.validate(any())).thenReturn(List.of());
        when(relatedRaidValidator.validate(any())).thenReturn(List.of());
        when(alternateIdentifierValidator.validateAlternateIdentifier(any())).thenReturn(List.of());
        when(contributorValidator.validate(any())).thenReturn(List.of());
        when(organisationValidator.validate(any())).thenReturn(List.of());
        when(relatedObjectValidator.validateRelatedObjects(any())).thenReturn(List.of());
        when(spatialCoverageValidator.validate(any())).thenReturn(List.of());
    }
}
