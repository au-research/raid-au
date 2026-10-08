package au.org.raid.api.config.bean;

import au.org.raid.api.client.contributor.isni.IsniClient;
import au.org.raid.api.client.contributor.isni.IsniRequestEntityFactory;
import au.org.raid.api.client.contributor.orcid.OrcidClient;
import au.org.raid.api.client.contributor.orcid.OrcidRequestEntityFactory;
import au.org.raid.api.client.repository.DataciteRepositoryClient;
import au.org.raid.api.client.ror.RorClient;
import au.org.raid.api.client.ror.RorRequestEntityFactory;
import au.org.raid.api.config.properties.DataciteProperties;
import au.org.raid.api.config.properties.RepositoryClientProperties;
import au.org.raid.api.config.properties.StubProperties;
import au.org.raid.api.factory.HttpEntityFactory;
import au.org.raid.api.factory.datacite.DataciteRequestFactory;
import au.org.raid.api.service.datacite.DataciteService;
import au.org.raid.api.service.doi.DoiService;
import au.org.raid.api.service.handle.HandleService;
import au.org.raid.api.service.rrid.RridService;
import au.org.raid.api.service.stub.*;
import au.org.raid.api.service.webarchive.WebArchiveService;
import au.org.raid.api.util.Log;
import au.org.raid.api.validator.GeoNamesUriValidator;
import au.org.raid.api.validator.OpenStreetMapUriValidator;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static au.org.raid.api.util.Log.to;

@Component
public class ExternalPidService {
    private static final Log log = to(ExternalPidService.class);

    @Bean
    @Primary
    public IsniClient isniClient(
            StubProperties stubProperties,
            IsniRequestEntityFactory isniRequestEntityFactory,
            RestTemplate restTemplate
    ) {
        if (stubProperties.getIsni() != null && stubProperties.getIsni().isEnabled()) {
            log.with("isniInMemoryStubDelay", stubProperties.getIsni().getDelay()).
                    warn("using the in-memory ISNI service");
            return new IsniClientStub(stubProperties.getIsni().getDelay());
        }

        return new IsniClient(restTemplate, isniRequestEntityFactory);
    }

    @Bean
    @Primary
    public OrcidClient orcidClient(
            StubProperties stubProperties,
            OrcidRequestEntityFactory orcidRequestEntityFactory,
            RestTemplate restTemplate
    ) {
        if (stubProperties.getOrcid() != null && stubProperties.getOrcid().isEnabled()) {
            log.with("orcidInMemoryStubDelay", stubProperties.getOrcid().getDelay()).
                    warn("using the in-memory ORCID client");
            return new OrcidClientStub(stubProperties.getOrcid().getDelay());
        }

        return new OrcidClient(orcidRequestEntityFactory, restTemplate);
    }

    @Bean
    @Primary
    public RorClient rorClient(
            StubProperties stubProperties,
            RorRequestEntityFactory rorRequestEntityFactory,
            @Qualifier("uriValidatorRestTemplate") RestTemplate restTemplate
    ) {
        if (stubProperties.getRor() != null && stubProperties.getRor().isEnabled()) {
            log.with("rorInMemoryStubDelay", stubProperties.getRor().getDelay()).
                    warn("using the in-memory ROR client");
            return new RorClientStub(stubProperties.getRor().getDelay());
        }

        return new RorClient(restTemplate, rorRequestEntityFactory);
    }

    @Bean
    @Primary
    public DoiService doiService(
            StubProperties stubProperties,
            @Qualifier("uriValidatorRestTemplate") RestTemplate restTemplate
    ) {
        if (stubProperties.getDoi().isEnabled()) {
            log.warn("using the in-memory DOI service");
            return new DoiServiceStub(stubProperties.getDoi().getDelay());
        }

        return new DoiService(restTemplate);
    }

    @Bean
    @Primary
    public HandleService handleService(
            StubProperties stubProperties,
            @Qualifier("uriValidatorRestTemplate") RestTemplate restTemplate
    ) {
        if (stubProperties.getHandle().isEnabled()) {
            log.warn("using the in-memory Handle service");
            return new HandleServiceStub(stubProperties.getHandle().getDelay());
        }

        return new HandleService(restTemplate);
    }

    @Bean
    @Primary
    public RridService rridService(
            StubProperties stubProperties,
            @Qualifier("uriValidatorRestTemplate") RestTemplate restTemplate
    ) {
        if (stubProperties.getRrid().isEnabled()) {
            log.warn("using the in-memory RRID service");
            return new RridServiceStub(stubProperties.getRrid().getDelay());
        }

        return new RridService(restTemplate);
    }

    @Bean
    @Primary
    public WebArchiveService webArchiveService(
            StubProperties stubProperties,
            @Qualifier("uriValidatorRestTemplate") RestTemplate restTemplate,
            Clock clock
    ) {
        if (stubProperties.getWebArchive() != null && stubProperties.getWebArchive().isEnabled()) {
            log.warn("using the in-memory Web Archive service");
            return new WebArchiveServiceStub(stubProperties.getWebArchive().getDelay());
        }

        return new WebArchiveService(restTemplate, clock);
    }

    @Bean
    @Primary
    public GeoNamesUriValidator geoNamesUriValidator(
            final StubProperties stubProperties,
            @Qualifier("uriValidatorRestTemplate") final RestTemplate restTemplate,
            @Value("${raid.validation.geonames.username}") final String username
    ) {
        if (stubProperties.getGeoNames().isEnabled()) {
            log.warn("using the in-memory GeoNames validator");
            return new GeonamesUriValidatorStub(stubProperties.getGeoNames().getDelay());
        }

        return new GeoNamesUriValidator(restTemplate,  username);
    }

    @Bean
    @Primary
    public OpenStreetMapUriValidator openStreetMapUriValidator(
            final StubProperties stubProperties,
            @Qualifier("uriValidatorRestTemplate") final RestTemplate restTemplate
    ) {
        if (stubProperties.getOpenStreetMap().isEnabled()) {
            log.warn("using the in-memory OpenStreetMap validator");
            return new OpenStreetMapValidatorStub(stubProperties.getOpenStreetMap().getDelay());
        }

        return new OpenStreetMapUriValidator(restTemplate);
    }

    @Bean
    @Primary
    public DataciteService dataciteService(
            final StubProperties stubProperties,
            @Value("${raid.environment}") final String environment,
            final DataciteProperties dataciteProperties,
            final RestTemplate restTemplate,
            final DataciteRequestFactory dataciteRequestFactory,
            final HttpEntityFactory httpEntityFactory,
            final ObjectMapper objectMapper
    ) {
        if (isDataciteStubEnabled(stubProperties, environment)) {
            log.warn("using the in-memory DataCite service; DOIs will NOT be minted or updated in DataCite");
            return new DataciteServiceStub(dataciteRequestFactory, objectMapper, stubProperties.getDatacite().getDelay());
        }

        return new DataciteService(dataciteProperties, restTemplate, dataciteRequestFactory, httpEntityFactory, objectMapper);
    }

    @Bean
    @Primary
    public DataciteRepositoryClient dataciteRepositoryClient(
            final StubProperties stubProperties,
            @Value("${raid.environment}") final String environment,
            final RestTemplate restTemplate,
            final RepositoryClientProperties repositoryClientProperties,
            final HttpEntityFactory httpEntityFactory
    ) {
        if (isDataciteStubEnabled(stubProperties, environment)) {
            log.warn("using the in-memory DataCite repository client; repositories will NOT be created in DataCite");
            return new DataciteRepositoryClientStub(stubProperties.getDatacite().getPrefix());
        }

        return new DataciteRepositoryClient(restTemplate, repositoryClientProperties, httpEntityFactory);
    }

    // Unlike the read-only validator stubs, this one silently drops writes, so refuse to start
    // rather than let a prod instance mint RAiDs with no DOI behind them.
    private static boolean isDataciteStubEnabled(final StubProperties stubProperties, final String environment) {
        final var enabled = stubProperties.getDatacite() != null && stubProperties.getDatacite().isEnabled();
        if (enabled && "prod".equalsIgnoreCase(environment)) {
            throw new IllegalStateException("raid.stub.datacite.enabled must not be true when raid.environment=prod");
        }
        return enabled;
    }

    @Bean
    public Map<String, BiFunction<String, String, List<ValidationFailure>>> spatialCoverageUriValidatorMap(
            final GeoNamesUriValidator geoNamesUriValidator,
            final OpenStreetMapUriValidator openStreetMapUriValidator,
            @Value("${raid.spatial-coverage.schema-uri.geonames}") final String geoNamesSchemaUri,
            @Value("${raid.spatial-coverage.schema-uri.openstreetmap}") final String openStreetMapSchemaUri
    ) {
        return Map.of(
                geoNamesSchemaUri, geoNamesUriValidator::validate,
                openStreetMapSchemaUri, openStreetMapUriValidator::validate
        );
    }
}
