package au.org.raid.api.controller;

import au.org.raid.api.dto.OrganisationCountDto;
import au.org.raid.api.dto.RaidCountDto;
import au.org.raid.api.endpoint.raidv2.RaidExceptionHandler;
import au.org.raid.api.exception.ResourceNotFoundException;
import au.org.raid.api.service.RaidHistoryService;
import au.org.raid.api.service.RaidIngestService;
import au.org.raid.api.service.ServicePointService;
import au.org.raid.api.service.raid.RaidService;
import au.org.raid.api.service.raid.id.IdentifierHandle;
import au.org.raid.api.service.raid.id.IdentifierUrl;
import au.org.raid.api.util.FileUtil;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.api.util.TokenUtil;
import au.org.raid.api.validator.ValidationService;
import au.org.raid.fixtures.APIFixtures;
import au.org.raid.idl.raidv2.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hamcrest.Matchers;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static au.org.raid.api.util.FileUtil.resourceContent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RaidControllerTest {
    public static final String REGISTRATION_AGENCY_ID = "https://ror.org/038sjwq14";
    public static final String IDENTIFIER_OWNER_ID = "https://ror.org/02stey378";

    private static final String SERVICE_POINT_GROUP_ID = UUID.randomUUID().toString();
    private static final Long SERVICE_POINT_ID = 20_000_000L;
    private static final String PREFIX = "10378.1";
    private static final String SUFFIX = "1696639";
    private static final String USER_ID = "user-id";

    final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
    private MockMvc mockMvc;
    @Mock
    private RaidService raidService;
    @Mock
    private ValidationService validationService;
    @Mock
    private RaidIngestService raidIngestService;
    @Mock
    private RaidHistoryService raidHistoryService;
    @Mock
    private ServicePointService servicePointService;
    @InjectMocks
    private RaidController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RaidExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void mintRaid_ReturnsRedactedInternalServerErrorOnDataAccessException() throws Exception {
        final var raid = APIFixtures.newCreateRequest();

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {

            final var jwt = getJwt();
            final var verifyFindServicePointId = mockFindServicePointId();

            final var jwtAuthenticationToken = mock(JwtAuthenticationToken.class);
            when(jwtAuthenticationToken.getToken()).thenReturn(jwt);

            final var securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            doThrow(DataAccessException.class)
                    .when(raidService).mint(any(RaidCreateRequest.class), eq(SERVICE_POINT_ID));

            mockMvc.perform(post("/raid/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(raid))
                            .characterEncoding("utf-8"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(""))
                    .andReturn();

            verifyFindServicePointId.get();
        }
    }

    @Test
    void mintRaid_ReturnsBadRequest() throws Exception {
        final var validationFailureMessage = "validation failure message";
        final var validationFailureType = "validation failure type";
        final var validationFailureFieldId = "validation failure id";

        final var raid = APIFixtures.newCreateRequest();

        final var validationFailure = new ValidationFailure();
        validationFailure.setFieldId(validationFailureFieldId);
        validationFailure.setMessage(validationFailureMessage);
        validationFailure.setErrorType(validationFailureType);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {

            final var jwt = getJwt();
            final var verifyFindServicePointId = mockFindServicePointId();

            final var jwtAuthenticationToken = mock(JwtAuthenticationToken.class);
            when(jwtAuthenticationToken.getToken()).thenReturn(jwt);

            final var securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            when(validationService.validateForCreate(any(RaidCreateRequest.class)))
                    .thenReturn(List.of(validationFailure));

            final MvcResult mvcResult = mockMvc.perform(post("/raid/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(raid))
                            .characterEncoding("utf-8"))
                    .andDo(print())
                    .andReturn();

            final ValidationFailureResponse validationFailureResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ValidationFailureResponse.class);

            assertThat(validationFailureResponse.getType(), Matchers.is("https://raid.org.au/errors#ValidationException"));
            assertThat(validationFailureResponse.getTitle(), Matchers.is("There were validation failures."));
            assertThat(validationFailureResponse.getStatus(), Matchers.is(400));
            assertThat(validationFailureResponse.getDetail(), Matchers.is("Request had 1 validation failure(s). See failures for more details..."));
            assertThat(validationFailureResponse.getInstance(), Matchers.is("https://raid.org.au"));
            assertThat(validationFailureResponse.getFailures().get(0).getFieldId(), Matchers.is(validationFailureFieldId));
            assertThat(validationFailureResponse.getFailures().get(0).getErrorType(), Matchers.is(validationFailureType));
            assertThat(validationFailureResponse.getFailures().get(0).getMessage(), Matchers.is(validationFailureMessage));

            verifyFindServicePointId.get();
            verify(raidService, never()).mint(any(RaidCreateRequest.class), eq(SERVICE_POINT_ID));
            verify(raidService, never()).findByHandle(anyString());
        }
    }

    @Test
    @WithMockUser(authorities = {"user"})
    void mintRaid_ReturnsOk() throws Exception {
        final var title = "test-title";
        final var startDate = LocalDate.now();
        final var handle = new IdentifierHandle("10378.1", "1696639");
        final var id = new IdentifierUrl("https://raid.org.au", handle);
        final var endDate = startDate.plusMonths(6);

        final var raidForPost = APIFixtures.newCreateRequest();
        final var raidForGet = createRaidForGet(title, startDate);

        when(validationService.validateForCreate(any(RaidCreateRequest.class))).thenReturn(Collections.emptyList());

        when(raidService.mint(any(RaidCreateRequest.class), eq(SERVICE_POINT_ID))).thenReturn(raidForGet);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {

            final var jwt = getJwt();
            final var verifyFindServicePointId = mockFindServicePointId();

            final var jwtAuthenticationToken = mock(JwtAuthenticationToken.class);
            when(jwtAuthenticationToken.getToken()).thenReturn(jwt);

            final var securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            mockMvc.perform(post("/raid/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(raidForPost))
                            .characterEncoding("utf-8"))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.identifier.id", Matchers.is(id.formatUrl())))
                    .andExpect(jsonPath("$.identifier.schemaUri", Matchers.is(SchemaValues.RAID_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$.identifier.registrationAgency.id", Matchers.is(REGISTRATION_AGENCY_ID)))
                    .andExpect(jsonPath("$.identifier.registrationAgency.schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$.identifier.owner.id", Matchers.is(IDENTIFIER_OWNER_ID)))
                    .andExpect(jsonPath("$.identifier.owner.schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$.identifier.owner.servicePoint", Matchers.is(SERVICE_POINT_ID.intValue())))
                    .andExpect(jsonPath("$.identifier.raidAgencyUrl", Matchers.is(id.formatUrl())))
                    .andExpect(jsonPath("$.title[0].text", Matchers.is(title)))
                    .andExpect(jsonPath("$.title[0].type.id", Matchers.is(SchemaValues.PRIMARY_TITLE_TYPE.getUri())))
                    .andExpect(jsonPath("$.title[0].type.schemaUri", Matchers.is(SchemaValues.TITLE_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$.title[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.title[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.date.startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.date.endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.description[0].text", Matchers.is("Genome sequencing and assembly project at WUR of the C. Japonicum. ")))
                    .andExpect(jsonPath("$.description[0].type.id", Matchers.is(SchemaValues.PRIMARY_DESCRIPTION_TYPE.getUri())))
                    .andExpect(jsonPath("$.description[0].type.schemaUri", Matchers.is(SchemaValues.DESCRIPTION_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$.access.type.id", Matchers.is(SchemaValues.ACCESS_TYPE_OPEN.getUri())))
                    .andExpect(jsonPath("$.access.embargoExpiry", Matchers.is("2024-01-01")))
                    .andExpect(jsonPath("$.access.type.schemaUri", Matchers.is(SchemaValues.ACCESS_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$.access.statement.text", Matchers.is("This RAiD is closed")))
                    .andExpect(jsonPath("$.contributor[0].id", Matchers.is("https://orcid.org/0000-0002-4368-8058")))
                    .andExpect(jsonPath("$.contributor[0].schemaUri", Matchers.is("https://orcid.org/")))
                    .andExpect(jsonPath("$.contributor[0].position[0].schemaUri", Matchers.is(SchemaValues.CONTRIBUTOR_POSITION_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$.contributor[0].position[0].id", Matchers.is(SchemaValues.PRINCIPAL_INVESTIGATOR_CONTRIBUTOR_POSITION_ROLE.getUri())))
                    .andExpect(jsonPath("$.contributor[0].position[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.contributor[0].position[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.contributor[0].role[0].schemaUri", Matchers.is(SchemaValues.CONTRIBUTOR_ROLE_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$.contributor[0].role[0].id", Matchers.is("https://credit.niso.org/contributor-roles/formal-analysis/")))
                    .andExpect(jsonPath("$.organisation[0].role[0].id", Matchers.is(SchemaValues.LEAD_RESEARCH_ORGANISATION_ROLE.getUri())))
                    .andExpect(jsonPath("$.organisation[0].role[0].schemaUri", Matchers.is(SchemaValues.ORGANISATION_ROLE_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$.organisation[0].role[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.organisation[0].role[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.organisation[0].id", Matchers.is("https://ror.org/04qw24q55")))
                    .andExpect(jsonPath("$.organisation[0].schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())));

            verifyFindServicePointId.get();
        }
    }

    @Test
    void updateRaid_ReturnsBadRequest() throws Exception {
        final var handle = String.join("/", PREFIX, SUFFIX);
        final var validationFailureMessage = "validation failure message";
        final var validationFailureType = "validation failure type";
        final var validationFailureFieldId = "validation failure id";

        final var input = APIFixtures.newUpdateRequest();

        final var validationFailure = new ValidationFailure();
        validationFailure.setFieldId(validationFailureFieldId);
        validationFailure.setMessage(validationFailureMessage);
        validationFailure.setErrorType(validationFailureType);

        when(validationService.validateForUpdate(eq(handle), any(RaidUpdateRequest.class))).thenReturn(List.of(validationFailure));

        final MvcResult mvcResult = mockMvc.perform(put(String.format("/raid/%s/%s", PREFIX, SUFFIX))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input))
                        .characterEncoding("utf-8"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();

        final ValidationFailureResponse validationFailureResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ValidationFailureResponse.class);

        assertThat(validationFailureResponse.getType(), Matchers.is("https://raid.org.au/errors#ValidationException"));
        assertThat(validationFailureResponse.getTitle(), Matchers.is("There were validation failures."));
        assertThat(validationFailureResponse.getStatus(), Matchers.is(400));
        assertThat(validationFailureResponse.getDetail(), Matchers.is("Request had 1 validation failure(s). See failures for more details..."));
        assertThat(validationFailureResponse.getInstance(), Matchers.is("https://raid.org.au"));
        assertThat(validationFailureResponse.getFailures().get(0).getFieldId(), Matchers.is(validationFailureFieldId));
        assertThat(validationFailureResponse.getFailures().get(0).getErrorType(), Matchers.is(validationFailureType));
        assertThat(validationFailureResponse.getFailures().get(0).getMessage(), Matchers.is(validationFailureMessage));

        verifyNoMoreInteractions(raidService);
    }

    @Test
    void updateRaid_ReturnsOk() throws Exception {
        final var title = "test-title";
        final var startDate = LocalDate.now();
        final var handle = new IdentifierHandle(PREFIX, SUFFIX);
        final var id = new IdentifierUrl("https://raid.org.au", handle);
        final var endDate = startDate.plusMonths(6);
        final var servicePointId = 20000000L;

        final var input = APIFixtures.newUpdateRequest();
        final var output = createRaidForGet(title, startDate);

        when(validationService.validateForUpdate(String.join("/", PREFIX, SUFFIX), input))
                .thenReturn(Collections.emptyList());

        when(raidService.update(input, servicePointId)).thenReturn(output);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {

            final var jwt = getJwt();
            final var verifyFindServicePointId = mockFindServicePointId(servicePointId);

            final var jwtAuthenticationToken = mock(JwtAuthenticationToken.class);
            when(jwtAuthenticationToken.getToken()).thenReturn(jwt);

            final var securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            mockMvc.perform(put(String.format("/raid/%s/%s", PREFIX, SUFFIX))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(input))
                            .characterEncoding("utf-8"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.identifier.id", Matchers.is(id.formatUrl())))
                    .andExpect(jsonPath("$.identifier.schemaUri", Matchers.is(SchemaValues.RAID_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$.identifier.registrationAgency.id", Matchers.is(REGISTRATION_AGENCY_ID)))
                    .andExpect(jsonPath("$.identifier.registrationAgency.schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$.identifier.owner.id", Matchers.is(IDENTIFIER_OWNER_ID)))
                    .andExpect(jsonPath("$.identifier.owner.schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$.identifier.owner.servicePoint", Matchers.is(SERVICE_POINT_ID.intValue())))
                    .andExpect(jsonPath("$.title[0].text", Matchers.is(title)))
                    .andExpect(jsonPath("$.title[0].type.id", Matchers.is(SchemaValues.PRIMARY_TITLE_TYPE.getUri())))
                    .andExpect(jsonPath("$.title[0].type.schemaUri", Matchers.is(SchemaValues.TITLE_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$.title[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.title[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.date.startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.date.endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.description[0].text", Matchers.is("Genome sequencing and assembly project at WUR of the C. Japonicum. ")))
                    .andExpect(jsonPath("$.description[0].type.id", Matchers.is(SchemaValues.PRIMARY_DESCRIPTION_TYPE.getUri())))
                    .andExpect(jsonPath("$.description[0].type.schemaUri", Matchers.is(SchemaValues.DESCRIPTION_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$.access.type.id", Matchers.is(SchemaValues.ACCESS_TYPE_OPEN.getUri())))
                    .andExpect(jsonPath("$.access.type.schemaUri", Matchers.is(SchemaValues.ACCESS_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$.access.statement.text", Matchers.is("This RAiD is closed")))
                    .andExpect(jsonPath("$.contributor[0].id", Matchers.is("https://orcid.org/0000-0002-4368-8058")))
                    .andExpect(jsonPath("$.contributor[0].schemaUri", Matchers.is("https://orcid.org/")))
                    .andExpect(jsonPath("$.contributor[0].position[0].schemaUri", Matchers.is(SchemaValues.CONTRIBUTOR_POSITION_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$.contributor[0].position[0].id", Matchers.is(SchemaValues.PRINCIPAL_INVESTIGATOR_CONTRIBUTOR_POSITION_ROLE.getUri())))
                    .andExpect(jsonPath("$.contributor[0].position[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.contributor[0].position[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.contributor[0].role[0].schemaUri", Matchers.is(SchemaValues.CONTRIBUTOR_ROLE_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$.contributor[0].role[0].id", Matchers.is("https://credit.niso.org/contributor-roles/formal-analysis/")))
                    .andExpect(jsonPath("$.organisation[0].role[0].id", Matchers.is(SchemaValues.LEAD_RESEARCH_ORGANISATION_ROLE.getUri())))
                    .andExpect(jsonPath("$.organisation[0].role[0].schemaUri", Matchers.is(SchemaValues.ORGANISATION_ROLE_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$.organisation[0].role[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.organisation[0].role[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$.organisation[0].id", Matchers.is("https://ror.org/04qw24q55")))
                    .andExpect(jsonPath("$.organisation[0].schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())));
            verifyFindServicePointId.get();
        }
    }

    @Test
    void updateRaid_Returns404IfNotFound() throws Exception {
        final var handle = String.join("/", PREFIX, SUFFIX);
        final var input = APIFixtures.newUpdateRequest();
        final var servicePointId = 20000000L;

        when(validationService.validateForUpdate(eq(handle), any(RaidUpdateRequest.class))).thenReturn(Collections.emptyList());

        doThrow(new ResourceNotFoundException(handle))
                .when(raidService).update(input, servicePointId);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {

            final var jwt = getJwt();
            final var verifyFindServicePointId = mockFindServicePointId(servicePointId);

            final var jwtAuthenticationToken = mock(JwtAuthenticationToken.class);
            when(jwtAuthenticationToken.getToken()).thenReturn(jwt);

            final var securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            final MvcResult mvcResult = mockMvc.perform(put(String.format("/raid/%s/%s", PREFIX, SUFFIX))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(input))
                            .characterEncoding("utf-8"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andReturn();

            final FailureResponse failureResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), FailureResponse.class);

            assertThat(failureResponse.getType(), Matchers.is("https://raid.org.au/errors#ResourceNotFoundException"));
            assertThat(failureResponse.getTitle(), Matchers.is("The resource was not found."));
            assertThat(failureResponse.getStatus(), Matchers.is(404));
            assertThat(failureResponse.getDetail(), Matchers.is("No RAiD was found with handle 10378.1/1696639."));
            assertThat(failureResponse.getInstance(), Matchers.is("https://raid.org.au"));

            verifyFindServicePointId.get();
        }
    }

    @Test
    @Disabled
    void updateRaids_ReturnsForbiddenWithInvalidServicePoint() throws Exception {
        final var input = createRaidForGet("", LocalDate.now());
        final var servicePointId = 20_000_001L;

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {

            final var jwt = getJwt();
            final var verifyFindServicePointId = mockFindServicePointId(servicePointId);

            final var jwtAuthenticationToken = mock(JwtAuthenticationToken.class);
            when(jwtAuthenticationToken.getToken()).thenReturn(jwt);

            final var securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            final MvcResult mvcResult = mockMvc.perform(put(String.format("/raid/%s/%s", PREFIX, SUFFIX))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(input))
                            .characterEncoding("utf-8"))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andReturn();

            final FailureResponse failureResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), FailureResponse.class);

            assertThat(failureResponse.getType(), Matchers.is("https://raid.org.au/errors#CrossAccountAccessException"));
            assertThat(failureResponse.getTitle(), Matchers.is("You do not have permission to access this RAiD."));
            assertThat(failureResponse.getStatus(), Matchers.is(403));
            assertThat(failureResponse.getDetail(), Matchers.is("You don't have permission to access RAiDs with a service point of " + servicePointId));
            assertThat(failureResponse.getInstance(), Matchers.is("https://raid.org.au"));

            verifyFindServicePointId.get();
            verifyNoInteractions(raidService);
        }
    }

    @Test
    void readRaid_ReturnsOk() throws Exception {
        final var startDate = LocalDate.now().minusYears(1);
        final var title = "test-title";
        final var raid = createRaidForGet(title, startDate);

        when(raidService.findByHandle(String.join("/", PREFIX, SUFFIX))).thenReturn(Optional.of(raid));

        final MvcResult mvcResult = mockMvc.perform(get(String.format("/raid/%s/%s", PREFIX, SUFFIX))
                        .characterEncoding("utf-8")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        final RaidDto result = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), RaidDto.class);

        assertThat(result, Matchers.is(raid));
    }

    @Test
    @Disabled()
    @DisplayName("Returns raid at given version")
    void readRaid_ReturnsRaidAtVersion() throws Exception {
        final var startDate = LocalDate.now().minusYears(1);
        final var title = "test-title";
        final var raid = createRaidForGet(title, startDate);
        final var version = 9;

        when(raidService.findByHandle(String.join("/", PREFIX, SUFFIX))).thenReturn(Optional.of(raid));
        when(raidHistoryService.findByHandleAndVersion(String.join("/", PREFIX, SUFFIX), version))
                .thenReturn(Optional.of(objectMapper.writeValueAsString(raid)));

        final MvcResult mvcResult = mockMvc.perform(get(String.format("/raid/%s/%s", PREFIX, SUFFIX))
                        .queryParam("version", String.valueOf(version))
                        .characterEncoding("utf-8")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        final RaidDto result = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), RaidDto.class);

        assertThat(result, Matchers.is(raid));
    }

    @Test
    @Disabled
    @DisplayName("Returns 404 when no raid found at given version")
    void readRaid_ReturnsNotFoundIfVersionNotFound() throws Exception {
        final var startDate = LocalDate.now().minusYears(1);
        final var title = "test-title";
        final var raid = createRaidForGet(title, startDate);
        final var version = 9;

        when(raidService.findByHandle(String.join("/", PREFIX, SUFFIX))).thenReturn(Optional.of(raid));
        when(raidHistoryService.findByHandleAndVersion(String.join("/", PREFIX, SUFFIX), version))
                .thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(get(String.format("/raid/%s/%s", PREFIX, SUFFIX))
                        .queryParam("version", String.valueOf(version))
                        .characterEncoding("utf-8")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    void readRaid_ReturnsNotFound() throws Exception {
        final var handle = String.join("/", PREFIX, SUFFIX);

        doThrow(new ResourceNotFoundException(handle)).when(raidService).findByHandle(handle);

        final MvcResult mvcResult = mockMvc.perform(get(String.format("/raid/%s/%s", PREFIX, SUFFIX))
                        .characterEncoding("utf-8")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andReturn();

        final FailureResponse failureResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), FailureResponse.class);

        assertThat(failureResponse.getType(), Matchers.is("https://raid.org.au/errors#ResourceNotFoundException"));
        assertThat(failureResponse.getTitle(), Matchers.is("The resource was not found."));
        assertThat(failureResponse.getStatus(), Matchers.is(404));
        assertThat(failureResponse.getDetail(), Matchers.is("No RAiD was found with handle %s/%s.".formatted(PREFIX, SUFFIX)));
        assertThat(failureResponse.getInstance(), Matchers.is("https://raid.org.au"));
    }

    @Test
    @Disabled("Test somewhere else")
    @DisplayName("Requesting a closed raid with a different service point returns forbidden response")
    void forbiddenWithClosedRaidAndDifferentServicePoint() throws Exception {
        final var raid = embargoedRaid();
        final var servicePointId = 20_000_001L;

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            final var jwt = getJwt();
            final var verifyFindServicePointId = mockFindServicePointId(servicePointId);

            final var jwtAuthenticationToken = mock(JwtAuthenticationToken.class);
            when(jwtAuthenticationToken.getToken()).thenReturn(jwt);

            final var securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            when(raidService.findByHandle(String.join("/", PREFIX, SUFFIX))).thenReturn(Optional.of(raid));

            mockMvc.perform(get(String.format("/raid/%s/%s", PREFIX, SUFFIX))
                            .characterEncoding("utf-8"))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.identifier.id", Matchers.is("https://raid.org.au/10378.1/1696639")))
                    .andExpect(jsonPath("$.access.type.id", Matchers.is(SchemaValues.ACCESS_TYPE_EMBARGOED.getUri())))
                    .andExpect(jsonPath("$.access.statement.text", Matchers.is("This RAiD is embargoed")))
                    .andExpect(jsonPath("$.access.statement.language.id", Matchers.is("eng")))
                    .andExpect(jsonPath("$.access.statement.language.schemaUri", Matchers.is("https://www.iso.org/standard/39534.html")))
                    .andReturn();

            verifyFindServicePointId.get();
        }
    }

    @Test
    @Disabled("Test somewhere else")
    @DisplayName("Requesting an embargoed raid with a different service point returns forbidden response")
    void forbiddenWithEmbargoedRaidAndDifferentServicePoint() throws Exception {
        final var raid = embargoedRaid();
        final var servicePoint = 20_000_001L;

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {

            final var jwt = getJwt();
            final var verifyFindServicePointId = mockFindServicePointId(servicePoint);

            final var jwtAuthenticationToken = mock(JwtAuthenticationToken.class);
            when(jwtAuthenticationToken.getToken()).thenReturn(jwt);

            final var securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            when(raidService.findByHandle(String.join("/", PREFIX, SUFFIX))).thenReturn(Optional.of(raid));

            mockMvc.perform(get(String.format("/raid/%s/%s", PREFIX, SUFFIX))
                            .characterEncoding("utf-8"))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.identifier.id", Matchers.is("https://raid.org.au/10378.1/1696639")))
                    .andExpect(jsonPath("$.access.type.id", Matchers.is(SchemaValues.ACCESS_TYPE_EMBARGOED.getUri())))
                    .andExpect(jsonPath("$.access.embargoExpiry", Matchers.is("2024-12-31")))
                    .andExpect(jsonPath("$.access.statement.text", Matchers.is("This RAiD is embargoed")))
                    .andExpect(jsonPath("$.access.statement.language.id", Matchers.is("eng")))
                    .andExpect(jsonPath("$.access.statement.language.schemaUri", Matchers.is("https://www.iso.org/standard/39534.html")))
                    .andReturn();

            verifyFindServicePointId.get();
        }
    }

    @Test
    void listRaids_ReturnsOk() throws Exception {
        final var title = "C. Japonicum Genome";
        final var startDate = LocalDate.now();
        final var handle = new IdentifierHandle("10378.1", "1696639");
        final var id = new IdentifierUrl("https://raid.org.au", handle);
        final var endDate = startDate.plusMonths(6);

        final var output = createRaidForGet(title, startDate);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {

            final var jwt = getJwt();
            final var verifyFindServicePointId = mockFindServicePointId();

            final var jwtAuthenticationToken = mock(JwtAuthenticationToken.class);
            when(jwtAuthenticationToken.getToken()).thenReturn(jwt);

            final var securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            
            when(raidIngestService.findAllByServicePointIdOrHandleIn(SERVICE_POINT_ID)).thenReturn(Collections.singletonList(output));

            mockMvc.perform(get("/raid/", handle)
                            .queryParam("servicePointId", SERVICE_POINT_ID.toString())
                            .characterEncoding("utf-8"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].identifier.id", Matchers.is(id.formatUrl())))
                    .andExpect(jsonPath("$[0].identifier.schemaUri", Matchers.is(SchemaValues.RAID_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].identifier.registrationAgency.id", Matchers.is(REGISTRATION_AGENCY_ID)))
                    .andExpect(jsonPath("$[0].identifier.registrationAgency.schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].identifier.owner.id", Matchers.is(IDENTIFIER_OWNER_ID)))
                    .andExpect(jsonPath("$[0].identifier.owner.schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].identifier.owner.servicePoint", Matchers.is(SERVICE_POINT_ID.intValue())))
                    .andExpect(jsonPath("$[0].title[0].text", Matchers.is(title)))
                    .andExpect(jsonPath("$[0].title[0].type.id", Matchers.is(SchemaValues.PRIMARY_TITLE_TYPE.getUri())))
                    .andExpect(jsonPath("$[0].title[0].type.schemaUri", Matchers.is(SchemaValues.TITLE_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$[0].title[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].title[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].date.startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].date.endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].description[0].text", Matchers.is("Genome sequencing and assembly project at WUR of the C. Japonicum. ")))
                    .andExpect(jsonPath("$[0].description[0].type.id", Matchers.is(SchemaValues.PRIMARY_DESCRIPTION_TYPE.getUri())))
                    .andExpect(jsonPath("$[0].description[0].type.schemaUri", Matchers.is( SchemaValues.DESCRIPTION_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$[0].access.type.id", Matchers.is(SchemaValues.ACCESS_TYPE_OPEN.getUri())))
                    .andExpect(jsonPath("$[0].access.type.schemaUri", Matchers.is(SchemaValues.ACCESS_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$[0].access.statement.text", Matchers.is("This RAiD is closed")))
                    .andExpect(jsonPath("$[0].contributor[0].id", Matchers.is("https://orcid.org/0000-0002-4368-8058")))
                    .andExpect(jsonPath("$[0].contributor[0].schemaUri", Matchers.is("https://orcid.org/")))
                    .andExpect(jsonPath("$[0].contributor[0].position[0].schemaUri", Matchers.is(SchemaValues.CONTRIBUTOR_POSITION_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].contributor[0].position[0].id", Matchers.is(SchemaValues.PRINCIPAL_INVESTIGATOR_CONTRIBUTOR_POSITION_ROLE.getUri())))
                    .andExpect(jsonPath("$[0].contributor[0].position[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].contributor[0].position[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].contributor[0].role[0].schemaUri", Matchers.is(SchemaValues.CONTRIBUTOR_ROLE_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].contributor[0].role[0].id", Matchers.is("https://credit.niso.org/contributor-roles/formal-analysis/")))
                    .andExpect(jsonPath("$[0].organisation[0].role[0].id", Matchers.is(SchemaValues.LEAD_RESEARCH_ORGANISATION_ROLE.getUri())))
                    .andExpect(jsonPath("$[0].organisation[0].role[0].schemaUri", Matchers.is(SchemaValues.ORGANISATION_ROLE_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].organisation[0].role[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].organisation[0].role[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].organisation[0].id", Matchers.is("https://ror.org/04qw24q55")))
                    .andExpect(jsonPath("$[0].organisation[0].schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())));
            verifyFindServicePointId.get();
        }
    }

    @Test
    @DisplayName("Raid history endpoint returns list of changes")
    void raidHistory() throws Exception {
        final var handle = PREFIX + "/" + SUFFIX;
        final var version = 1;
        final var diff = "_diff";
        final var timestamp = LocalDateTime.now().atOffset(ZoneOffset.UTC);

        final var raid = createRaidForGet("title", LocalDate.now());
        raid.getAccess().getType().setId(SchemaValues.ACCESS_TYPE_OPEN.getUri());

        final var raidChange = new RaidChange()
                .handle(handle)
                .version(version)
                .diff(diff)
                .timestamp(timestamp);

        when(raidHistoryService.findAllChangesByHandle(handle)).thenReturn(List.of(raidChange));

        mockMvc.perform(get(String.format("/raid/%s/%s/history", PREFIX, SUFFIX), handle)
                        .characterEncoding("utf-8"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].handle", Matchers.is(handle)))
                .andExpect(jsonPath("$[0].version", Matchers.is(version)))
                .andExpect(jsonPath("$[0].diff", Matchers.is(diff)))
                .andExpect(jsonPath("$[0].timestamp", Matchers.is(timestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))));
    }

    @Test
    @Disabled
    @DisplayName("Raid history returns 403 if raid is embargoed and user is from different service point")
    void raidHistoryEmbargoed() throws Exception {
        final var prefix = "_prefix";
        final var suffix = "_suffix";
        final var raid = embargoedRaid();
        final var handle = PREFIX + "/" + SUFFIX;
        final var servicePointId = 20_000_001L;

        when(raidService.findByHandle(handle)).thenReturn(Optional.of(raid));

        mockMvc.perform(get(String.format("/raid/%s/%s/history", PREFIX, SUFFIX), handle)
                        .characterEncoding("utf-8"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("Should return all raids containing a given organisation")
    void findAllByOrganisationId() throws Exception {
        final var title = "C. Japonicum Genome";
        final var startDate = LocalDate.now();
        final var ror = "https://ror.org/038sjwq14";
        final var handle = new IdentifierHandle("10378.1", "1696639");
        final var id = new IdentifierUrl("https://raid.org.au", handle);
        final var endDate = startDate.plusMonths(6);

        final var output = createRaidForGet(title, startDate);

        when(raidIngestService.findAllByOrganisation(ror)).thenReturn(Collections.singletonList(output));
        try (MockedStatic<TokenUtil> tokenUtil = Mockito.mockStatic(TokenUtil.class)) {
            tokenUtil.when(TokenUtil::getUserId).thenReturn(USER_ID);


            mockMvc.perform(get("/raid/").queryParam("organisation.id", ror))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].identifier.id", Matchers.is(id.formatUrl())))
                    .andExpect(jsonPath("$[0].identifier.schemaUri", Matchers.is(SchemaValues.RAID_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].identifier.registrationAgency.id", Matchers.is(REGISTRATION_AGENCY_ID)))
                    .andExpect(jsonPath("$[0].identifier.registrationAgency.schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].identifier.owner.id", Matchers.is(IDENTIFIER_OWNER_ID)))
                    .andExpect(jsonPath("$[0].identifier.owner.schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].identifier.owner.servicePoint", Matchers.is(SERVICE_POINT_ID.intValue())))
                    .andExpect(jsonPath("$[0].title[0].text", Matchers.is(title)))
                    .andExpect(jsonPath("$[0].title[0].type.id", Matchers.is(SchemaValues.PRIMARY_TITLE_TYPE.getUri())))
                    .andExpect(jsonPath("$[0].title[0].type.schemaUri", Matchers.is(SchemaValues.TITLE_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$[0].title[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].title[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].date.startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].date.endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].description[0].text", Matchers.is("Genome sequencing and assembly project at WUR of the C. Japonicum. ")))
                    .andExpect(jsonPath("$[0].description[0].type.id", Matchers.is(SchemaValues.PRIMARY_DESCRIPTION_TYPE.getUri())))
                    .andExpect(jsonPath("$[0].description[0].type.schemaUri", Matchers.is(SchemaValues.DESCRIPTION_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$[0].access.type.id", Matchers.is(SchemaValues.ACCESS_TYPE_OPEN.getUri())))
                    .andExpect(jsonPath("$[0].access.type.schemaUri", Matchers.is(SchemaValues.ACCESS_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$[0].access.statement.text", Matchers.is("This RAiD is closed")))
                    .andExpect(jsonPath("$[0].contributor[0].id", Matchers.is("https://orcid.org/0000-0002-4368-8058")))
                    .andExpect(jsonPath("$[0].contributor[0].schemaUri", Matchers.is("https://orcid.org/")))
                    .andExpect(jsonPath("$[0].contributor[0].position[0].schemaUri", Matchers.is(SchemaValues.CONTRIBUTOR_POSITION_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].contributor[0].position[0].id", Matchers.is(SchemaValues.PRINCIPAL_INVESTIGATOR_CONTRIBUTOR_POSITION_ROLE.getUri())))
                    .andExpect(jsonPath("$[0].contributor[0].position[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].contributor[0].position[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].contributor[0].role[0].schemaUri", Matchers.is(SchemaValues.CONTRIBUTOR_ROLE_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].contributor[0].role[0].id", Matchers.is("https://credit.niso.org/contributor-roles/formal-analysis/")))
                    .andExpect(jsonPath("$[0].organisation[0].role[0].id", Matchers.is(SchemaValues.LEAD_RESEARCH_ORGANISATION_ROLE.getUri())))
                    .andExpect(jsonPath("$[0].organisation[0].role[0].schemaUri", Matchers.is(SchemaValues.ORGANISATION_ROLE_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].organisation[0].role[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].organisation[0].role[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].organisation[0].id", Matchers.is("https://ror.org/04qw24q55")))
                    .andExpect(jsonPath("$[0].organisation[0].schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())));
        }
    }

    @Test
    @DisplayName("Should return all raids containing a given contributor")
    void findAllByContributorId() throws Exception {
        final var title = "C. Japonicum Genome";
        final var startDate = LocalDate.now();
        final var orcid = "https://orcid.org/0009-0006-4129-5257";
        final var handle = new IdentifierHandle("10378.1", "1696639");
        final var id = new IdentifierUrl("https://raid.org.au", handle);
        final var endDate = startDate.plusMonths(6);

        final var output = createRaidForGet(title, startDate);

        when(raidIngestService.findAllByContributor(orcid)).thenReturn(Collections.singletonList(output));

        try (MockedStatic<TokenUtil> tokenUtil = Mockito.mockStatic(TokenUtil.class)) {
            tokenUtil.when(TokenUtil::getUserId).thenReturn(USER_ID);

            mockMvc.perform(get("/raid/").queryParam("contributor.id", orcid))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].identifier.id", Matchers.is(id.formatUrl())))
                    .andExpect(jsonPath("$[0].identifier.schemaUri", Matchers.is(SchemaValues.RAID_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].identifier.registrationAgency.id", Matchers.is(REGISTRATION_AGENCY_ID)))
                    .andExpect(jsonPath("$[0].identifier.registrationAgency.schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].identifier.owner.id", Matchers.is(IDENTIFIER_OWNER_ID)))
                    .andExpect(jsonPath("$[0].identifier.owner.schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].identifier.owner.servicePoint", Matchers.is(SERVICE_POINT_ID.intValue())))
                    .andExpect(jsonPath("$[0].title[0].text", Matchers.is(title)))
                    .andExpect(jsonPath("$[0].title[0].type.id", Matchers.is(SchemaValues.PRIMARY_TITLE_TYPE.getUri())))
                    .andExpect(jsonPath("$[0].title[0].type.schemaUri", Matchers.is(SchemaValues.TITLE_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$[0].title[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].title[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].date.startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].date.endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].description[0].text", Matchers.is("Genome sequencing and assembly project at WUR of the C. Japonicum. ")))
                    .andExpect(jsonPath("$[0].description[0].type.id", Matchers.is(SchemaValues.PRIMARY_DESCRIPTION_TYPE.getUri())))
                    .andExpect(jsonPath("$[0].description[0].type.schemaUri", Matchers.is(SchemaValues.DESCRIPTION_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$[0].access.type.id", Matchers.is(SchemaValues.ACCESS_TYPE_OPEN.getUri())))
                    .andExpect(jsonPath("$[0].access.type.schemaUri", Matchers.is(SchemaValues.ACCESS_TYPE_SCHEMA.getUri())))
                    .andExpect(jsonPath("$[0].access.statement.text", Matchers.is("This RAiD is closed")))
                    .andExpect(jsonPath("$[0].contributor[0].id", Matchers.is("https://orcid.org/0000-0002-4368-8058")))
                    .andExpect(jsonPath("$[0].contributor[0].schemaUri", Matchers.is("https://orcid.org/")))
                    .andExpect(jsonPath("$[0].contributor[0].position[0].schemaUri", Matchers.is(SchemaValues.CONTRIBUTOR_POSITION_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].contributor[0].position[0].id", Matchers.is(SchemaValues.PRINCIPAL_INVESTIGATOR_CONTRIBUTOR_POSITION_ROLE.getUri())))
                    .andExpect(jsonPath("$[0].contributor[0].position[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].contributor[0].position[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].contributor[0].role[0].schemaUri", Matchers.is(SchemaValues.CONTRIBUTOR_ROLE_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].contributor[0].role[0].id", Matchers.is("https://credit.niso.org/contributor-roles/formal-analysis/")))
                    .andExpect(jsonPath("$[0].organisation[0].role[0].id", Matchers.is(SchemaValues.LEAD_RESEARCH_ORGANISATION_ROLE.getUri())))
                    .andExpect(jsonPath("$[0].organisation[0].role[0].schemaUri", Matchers.is(SchemaValues.ORGANISATION_ROLE_SCHEMA_URI.getUri())))
                    .andExpect(jsonPath("$[0].organisation[0].role[0].startDate", Matchers.is(startDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].organisation[0].role[0].endDate", Matchers.is(endDate.format(DateTimeFormatter.ISO_DATE))))
                    .andExpect(jsonPath("$[0].organisation[0].id", Matchers.is("https://ror.org/04qw24q55")))
                    .andExpect(jsonPath("$[0].organisation[0].schemaUri", Matchers.is(SchemaValues.ROR_SCHEMA_URI.getUri())));
        }
    }


    @Test
    @DisplayName("Count endpoint returns total count with no filters")
    void countRaids_ReturnsCountWithNoFilters() throws Exception {
        final var org = OrganisationCountDto.builder()
                .id("https://ror.org/038sjwq14")
                .name("Australian Research Data Commons")
                .count(42)
                .build();

        final var expectedCount = RaidCountDto.builder()
                .count(42)
                .organisations(List.of(org))
                .build();

        when(raidService.countRaids(null, null, null)).thenReturn(expectedCount);

        mockMvc.perform(get("/raid/count")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", Matchers.is(42)))
                .andExpect(jsonPath("$.organisations[0].id", Matchers.is("https://ror.org/038sjwq14")))
                .andExpect(jsonPath("$.organisations[0].name", Matchers.is("Australian Research Data Commons")))
                .andExpect(jsonPath("$.organisations[0].count", Matchers.is(42)));
    }

    @Test
    @DisplayName("Count endpoint returns count filtered by service point with name")
    void countRaids_ReturnsCountFilteredByServicePoint() throws Exception {
        final var servicePointId = 20000001L;
        final var org = OrganisationCountDto.builder()
                .id("https://ror.org/038sjwq14")
                .name("Australian Research Data Commons")
                .count(15)
                .build();

        final var expectedCount = RaidCountDto.builder()
                .count(15)
                .servicePointId(servicePointId)
                .servicePointName("Australian Research Data Commons")
                .organisations(List.of(org))
                .build();

        when(raidService.countRaids(eq(servicePointId), isNull(), isNull()))
                .thenReturn(expectedCount);

        mockMvc.perform(get("/raid/count")
                        .param("servicePointId", String.valueOf(servicePointId))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", Matchers.is(15)))
                .andExpect(jsonPath("$.servicePointId", Matchers.is(20000001)))
                .andExpect(jsonPath("$.servicePointName", Matchers.is("Australian Research Data Commons")))
                .andExpect(jsonPath("$.organisations[0].id", Matchers.is("https://ror.org/038sjwq14")))
                .andExpect(jsonPath("$.organisations[0].count", Matchers.is(15)));
    }

    @Test
    @DisplayName("Count endpoint returns count filtered by date range")
    void countRaids_ReturnsCountFilteredByDateRange() throws Exception {
        final var startDate = LocalDate.of(2025, 1, 1);
        final var endDate = LocalDate.of(2025, 6, 30);
        final var expectedCount = RaidCountDto.builder()
                .count(25)
                .startDate(startDate)
                .endDate(endDate)
                .organisations(Collections.emptyList())
                .build();

        when(raidService.countRaids(isNull(), eq(startDate), eq(endDate)))
                .thenReturn(expectedCount);

        mockMvc.perform(get("/raid/count")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-06-30")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", Matchers.is(25)))
                .andExpect(jsonPath("$.startDate", Matchers.is("2025-01-01")))
                .andExpect(jsonPath("$.endDate", Matchers.is("2025-06-30")))
                .andExpect(jsonPath("$.organisations", Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("Count endpoint returns count with all filters applied and multiple organisations")
    void countRaids_ReturnsCountWithAllFilters() throws Exception {
        final var servicePointId = 20000001L;
        final var startDate = LocalDate.of(2025, 1, 1);
        final var endDate = LocalDate.of(2025, 6, 30);

        final var org1 = OrganisationCountDto.builder()
                .id("https://ror.org/038sjwq14")
                .name("Australian Research Data Commons")
                .count(3)
                .build();
        final var org2 = OrganisationCountDto.builder()
                .id("https://ror.org/04qw24q55")
                .name("Wageningen University")
                .count(7)
                .build();

        final var expectedCount = RaidCountDto.builder()
                .count(10)
                .servicePointId(servicePointId)
                .servicePointName("Test Service Point")
                .startDate(startDate)
                .endDate(endDate)
                .organisations(List.of(org1, org2))
                .build();

        when(raidService.countRaids(eq(servicePointId), eq(startDate), eq(endDate)))
                .thenReturn(expectedCount);

        mockMvc.perform(get("/raid/count")
                        .param("servicePointId", String.valueOf(servicePointId))
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-06-30")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", Matchers.is(10)))
                .andExpect(jsonPath("$.servicePointId", Matchers.is(20000001)))
                .andExpect(jsonPath("$.servicePointName", Matchers.is("Test Service Point")))
                .andExpect(jsonPath("$.startDate", Matchers.is("2025-01-01")))
                .andExpect(jsonPath("$.endDate", Matchers.is("2025-06-30")))
                .andExpect(jsonPath("$.organisations", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.organisations[0].id", Matchers.is("https://ror.org/038sjwq14")))
                .andExpect(jsonPath("$.organisations[0].name", Matchers.is("Australian Research Data Commons")))
                .andExpect(jsonPath("$.organisations[0].count", Matchers.is(3)))
                .andExpect(jsonPath("$.organisations[1].id", Matchers.is("https://ror.org/04qw24q55")))
                .andExpect(jsonPath("$.organisations[1].name", Matchers.is("Wageningen University")))
                .andExpect(jsonPath("$.organisations[1].count", Matchers.is(7)));
    }

    private RaidDto createRaidForGet(final String title, final LocalDate startDate) throws IOException {
        final String json = FileUtil.resourceContent("/fixtures/raid.json");

        var raid = objectMapper.readValue(json, RaidDto.class);

        final var titleType = new TitleType()
                .id(SchemaValues.PRIMARY_TITLE_TYPE.getUri())
                .schemaUri(SchemaValues.TITLE_TYPE_SCHEMA.getUri());

        raid.getTitle().get(0)
                .startDate(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(startDate.plusMonths(6).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .text(title)
                .type(titleType);

        raid.getDate()
                .startDate(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(startDate.plusMonths(6).format(DateTimeFormatter.ISO_LOCAL_DATE));

        raid.getContributor().get(0).getPosition().get(0)
                .startDate(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(startDate.plusMonths(6).format(DateTimeFormatter.ISO_LOCAL_DATE));

        raid.getOrganisation().get(0).getRole().get(0)
                .startDate(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(startDate.plusMonths(6).format(DateTimeFormatter.ISO_LOCAL_DATE));

        return raid;
    }

    private RaidDto embargoedRaid() throws IOException {
        final String json = resourceContent("/fixtures/embargoed-raid.json");
        return objectMapper.readValue(json, RaidDto.class);
    }

    private Jwt getJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim(JwtClaimNames.SUB, "user")
                .claim("scope", "read")
                .claim("service_point_group_id", SERVICE_POINT_GROUP_ID)
                .build();
    }

    private Supplier<Void> mockFindServicePointId() {
        return mockFindServicePointId(SERVICE_POINT_ID);
    }

    private Supplier<Void> mockFindServicePointId(final long servicePointId) {
        when(servicePointService.findByGroupId(SERVICE_POINT_GROUP_ID))
                .thenReturn(Optional.of(new ServicePoint().id(servicePointId)));

        return () -> {
            verify(servicePointService).findByGroupId(SERVICE_POINT_GROUP_ID);
            return null;
        };
    }
}