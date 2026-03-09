package au.org.raid.api.validator;

import au.org.raid.api.service.raid.id.IdentifierParser;
import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.concurrent.Executor;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RaidoStableV1ValidatorTest {

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    @Mock
    private SubjectValidator subjectValidationService;
    @Mock
    private TitleValidator titleValidationService;
    @Mock
    private DescriptionValidator descSvc;
    @Mock
    private ContributorValidator contribSvc;
    @Mock
    private OrganisationValidator orgSvc;
    @Mock
    private IdentifierParser identifierParser;
    @Mock
    private RelatedRaidValidator relatedRaidValidationService;
    @Mock
    private RelatedObjectValidator relatedObjectValidationService;
    @Mock
    private AlternateIdentifierValidator alternateIdentifierValidationService;
    @Mock
    private SpatialCoverageValidator spatialCoverageValidationService;
    @Mock
    private AccessValidator accessValidationService;
    @Mock
    private DateValidator dateValidator;

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService(
                titleValidationService,
                descSvc,
                contribSvc,
                orgSvc,
                accessValidationService,
                subjectValidationService,
                identifierParser,
                relatedObjectValidationService,
                alternateIdentifierValidationService,
                relatedRaidValidationService,
                spatialCoverageValidationService,
                dateValidator,
                DIRECT_EXECUTOR
        );
    }

    @Test
    void validatesAccessOnCreate() {
        final var access = new Access();
        final var raid = new RaidCreateRequest().access(access);

        validationService.validateForCreate(raid);

        verify(accessValidationService).validate(access);
    }

    @Test
    void validatesAccessOnUpdate() {
        final var handle = "test-handle";
        final var access = new Access();

        final var raid = new RaidUpdateRequest()
                .identifier(new Id())
                .access(access);

        validationService.validateForUpdate(handle, raid);

        verify(accessValidationService).validate(access);
    }

    @Test
    void validatesSubjectsOnCreate() {
        final var subjects = Collections.singletonList(new Subject());
        final var raid = new RaidCreateRequest().subject(subjects);

        validationService.validateForCreate(raid);

        verify(subjectValidationService).validate(subjects);
    }

    @Test
    void validatesSubjectsOnUpdate() {
        final var handle = "test-handle";
        final var subjects = Collections.singletonList(new Subject());

        final var raid = new RaidUpdateRequest()
                .identifier(new Id())
                .subject(subjects);

        validationService.validateForUpdate(handle, raid);

        verify(subjectValidationService).validate(subjects);
    }

    @Test
    void validatesRelatedRaidsOnCreate() {
        final var relatedRaids = Collections.singletonList(new RelatedRaid());
        final var raid = new RaidCreateRequest().relatedRaid(relatedRaids);

        validationService.validateForCreate(raid);
        verify(relatedRaidValidationService).validate(relatedRaids);
    }

    @Test
    void validatesRelatedObjectsOnCreate() {
        final var relatedObjects = Collections.singletonList(new RelatedObject());
        final var raid = new RaidCreateRequest().relatedObject(relatedObjects);

        validationService.validateForCreate(raid);
        verify(relatedObjectValidationService).validateRelatedObjects(relatedObjects);
    }

    @Test
    void validatesRelatedRaidsOnUpdate() {
        final var handle = "test-handle";
        final var relatedRaids = Collections.singletonList(new RelatedRaid());

        final var raid = new RaidUpdateRequest()
                .identifier(new Id())
                .relatedRaid(relatedRaids);

        validationService.validateForUpdate(handle, raid);
        verify(relatedRaidValidationService).validate(relatedRaids);
    }

    @Test
    void validatesRelatedObjectsOnUpdate() {
        final var handle = "test-handle";
        final var relatedObjects = Collections.singletonList(new RelatedObject());

        final var raid = new RaidUpdateRequest()
                .identifier(new Id())
                .relatedObject(relatedObjects);

        validationService.validateForUpdate(handle, raid);
        verify(relatedObjectValidationService).validateRelatedObjects(relatedObjects);
    }

    @Test
    void validatesAlternateIdentifiersOnCreate() {
        final var alternateIdentifiers = Collections.singletonList(new AlternateIdentifier());

        final var raid = new RaidCreateRequest()
                .identifier(new Id())
                .alternateIdentifier(alternateIdentifiers);

        validationService.validateForCreate(raid);
        verify(alternateIdentifierValidationService).validateAlternateIdentifier(alternateIdentifiers);
    }

    @Test
    void validatesAlternateIdentifiersOnUpdate() {
        final var handle = "test-handle";
        final var alternateIdentifiers = Collections.singletonList(new AlternateIdentifier());

        final var raid = new RaidUpdateRequest()
                .identifier(new Id())
                .alternateIdentifier(alternateIdentifiers);

        validationService.validateForUpdate(handle, raid);
        verify(alternateIdentifierValidationService).validateAlternateIdentifier(alternateIdentifiers);
    }

    @Test
    void validatesSpatialCoverageOnCreate() {
        final var spatialCoverages =
                Collections.singletonList(new SpatialCoverage());

        final var raid = new RaidCreateRequest()
                .identifier(new Id())
                .spatialCoverage(spatialCoverages);

        validationService.validateForCreate(raid);
        verify(spatialCoverageValidationService).validate(spatialCoverages);
    }

    @Test
    void validatesSpatialCoverageOnUpdate() {
        final var handle = "test-handle";
        final var spatialCoverages =
                Collections.singletonList(new SpatialCoverage());

        final var raid = new RaidUpdateRequest()
                .identifier(new Id())
                .spatialCoverage(spatialCoverages);

        validationService.validateForUpdate(handle, raid);
        verify(spatialCoverageValidationService).validate(spatialCoverages);
    }
}
