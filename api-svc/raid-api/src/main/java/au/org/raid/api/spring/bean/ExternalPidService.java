package au.org.raid.api.spring.bean;

import au.org.raid.api.service.apids.ApidsService;
import au.org.raid.api.service.doi.DoiService;
import au.org.raid.api.service.orcid.OrcidService;
import au.org.raid.api.service.ror.RorService;
import au.org.raid.api.service.stub.*;
import au.org.raid.api.spring.config.environment.ApidsProps;
import au.org.raid.api.spring.config.environment.StubProperties;
import au.org.raid.api.util.Guard;
import au.org.raid.api.util.Log;
import au.org.raid.api.validator.GeoNamesUriValidator;
import au.org.raid.api.validator.OpenStreetMapUriValidator;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static au.org.raid.api.util.Log.to;

@Component
public class ExternalPidService {
    private static final Log log = to(ExternalPidService.class);

    @Bean
    @Primary
    public ApidsService apidsService(
            StubProperties stubProperties,
            ApidsProps apidsConfig,
            RestTemplate rest
    ) {
        /* IMPROVE: I'm fairly sure I'm not doing this the "spring way" */
        if (stubProperties.getApids().isEnabled()) {
            log.with("apidsInMemoryStubDelay", stubProperties.getApids().getDelay()).
                    warn("using the in-memory ORCID service");

            return new ApidsServiceStub(stubProperties.getApids());
        }

    /* now we aren't forced to set the secret if we're not using the real 
    APIDS service - unexpected benefit! */
        Guard.allHaveValue("must set ApidsProps values",
                apidsConfig.secret, apidsConfig.appId, apidsConfig.serviceUrl);
        return new ApidsService(apidsConfig, rest);
    }

    @Bean
    @Primary
    public OrcidService orcidService(
            StubProperties stubProperties,
            RestTemplate rest
    ) {
        if (stubProperties.getOrcid().isEnabled()) {
            log.with("orcidInMemoryStubDelay", stubProperties.getOrcid().getDelay()).
                    warn("using the in-memory ORCID service");
            return new OrcidServiceStub(stubProperties.getOrcid().getDelay());
        }

        return new OrcidService(rest);
    }

    @Bean
    @Primary
    public RorService rorService(
            StubProperties stubProperties,
            RestTemplate restTemplate
    ) {
        if (stubProperties.getRor().isEnabled()) {
            log.with("rorInMemoryStubDelay", stubProperties.getRor().getDelay()).
                    warn("using the in-memory ROR service");
            return new RorServiceStub(stubProperties.getRor().getDelay());
        }

        return new RorService(restTemplate);
    }


    @Bean
    @Primary
    public DoiService doiService(
            StubProperties stubProperties,
            RestTemplate restTemplate
    ) {
        if (stubProperties.getDoi().isEnabled()) {
            log.warn("using the in-memory DOI service");
            return new DoiServiceStub(stubProperties.getDoi().getDelay());
        }

        return new DoiService(restTemplate);
    }

    @Bean
    @Primary
    public GeoNamesUriValidator geoNamesUriValidator(
            final StubProperties stubProperties,
            final RestTemplate restTemplate,
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
            final RestTemplate restTemplate
    ) {
        if (stubProperties.getOpenStreetMap().isEnabled()) {
            log.warn("using the in-memory OpenStreetMap validator");
            return new OpenStreetMapValidatorStub(stubProperties.getOpenStreetMap().getDelay());
        }

        return new OpenStreetMapUriValidator(restTemplate);
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
