package au.org.raid.inttest;

import au.org.raid.api.Api;
import au.org.raid.api.service.stub.util.IdFactory;
import au.org.raid.api.spring.config.environment.EnvironmentProps;
import au.org.raid.idl.raidv2.api.AdminExperimentalApi;
import au.org.raid.idl.raidv2.api.RaidoStableV1Api;
import au.org.raid.idl.raidv2.api.UnapprovedExperimentalApi;
import au.org.raid.inttest.auth.BootstrapAuthTokenService;
import au.org.raid.inttest.config.IntTestProps;
import au.org.raid.inttest.config.IntegrationTestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.Logger.Level;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;

import static au.org.raid.api.endpoint.raidv2.AuthzUtil.RAIDO_SP_ID;
import static au.org.raid.api.spring.config.RaidWebSecurityConfig.RAID_V1_API;
import static au.org.raid.db.jooq.enums.UserRole.OPERATOR;
import static au.org.raid.inttest.config.IntegrationTestConfig.REST_TEMPLATE_VALUES_ONLY_ENCODING;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@SpringBootTest(classes = Api.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ContextConfiguration(classes = IntegrationTestConfig.class)
public abstract class IntegrationTestCase {
    // be careful, 25 char max column length
    public static final String INT_TEST_ROR = "https://ror.org/038sjwq14";
    protected static final IdFactory idFactory = new IdFactory("inttest");
//    @RegisterExtension
//    protected static JettyTestServer jettyTestServer = new JettyTestServer();
    @Autowired
    protected RestTemplate rest;
    @Autowired
    @Qualifier(REST_TEMPLATE_VALUES_ONLY_ENCODING)
    protected RestTemplate valuesEncodingRest;
    @Autowired
    protected IntTestProps props;
    @Autowired
    protected DSLContext db;
    @Autowired
    protected BootstrapAuthTokenService bootstrapTokenSvc;
    @Autowired
    protected Contract feignContract;
    @Autowired
    protected ObjectMapper mapper;
    @Autowired
    protected EnvironmentProps env;
    protected String raidV1TestToken;
    protected String operatorToken;
    private TestInfo testInfo;

    /**
     * IMPROVE: do this in a beforeAll, so the time isn't counted as
     * part of the first tests execution time.  But need to figure out how to
     * obtain the test's spring context in a static context.
     */
    @BeforeEach
    public void setupTestToken() {
        if (raidV1TestToken != null) {
            return;
        }

        raidV1TestToken = bootstrapTokenSvc.initRaidV1TestToken();
        operatorToken = bootstrapTokenSvc.bootstrapToken(
                RAIDO_SP_ID, "intTestOperatorApiToken", OPERATOR);

    }

    @BeforeEach
    public void init(TestInfo testInfo) {
        this.testInfo = testInfo;
    }

    public String getName() {
        return testInfo.getDisplayName().
                // the brackets aren't filename safe
                        replaceAll("[()]", "");
    }

    public UnapprovedExperimentalApi unapprovedClient(String token) {
        return Feign.builder()
                .client(new OkHttpClient())
                .encoder(new JacksonEncoder(mapper))
                .decoder(new ResponseEntityDecoder(new JacksonDecoder(mapper)))
                .contract(feignContract)
                .requestInterceptor(request ->
                        request.header(AUTHORIZATION, "Bearer " + token))
                .logger(new Slf4jLogger(UnapprovedExperimentalApi.class))
                .logLevel(Level.FULL)
                .target(UnapprovedExperimentalApi.class, props.getRaidoServerUrl());
    }

    public AdminExperimentalApi adminExperimentalClientAs(String token) {
        return Feign.builder()
                .client(new OkHttpClient())
                .encoder(new JacksonEncoder(mapper))
                .decoder(new ResponseEntityDecoder(new JacksonDecoder(mapper)))
                .contract(feignContract)
                .requestInterceptor(request ->
                        request.header(AUTHORIZATION, "Bearer " + token))
                .logger(new Slf4jLogger(AdminExperimentalApi.class))
                .logLevel(Level.FULL)
                .target(AdminExperimentalApi.class, props.getRaidoServerUrl());
    }

    public String raidoApiServerUrl(String url) {
        //noinspection HttpUrlsUsage
        return String.format("http://%s%s", props.raidoApiServer, url);
    }


    public RaidoStableV1Api basicRaidStableClient(String token) {
        return Feign.builder().
                client(new OkHttpClient()).
                encoder(new JacksonEncoder(mapper)).
                decoder(new JacksonDecoder(mapper)).
                errorDecoder(new RaidApiExceptionDecoder(mapper)).
                contract(feignContract).
                requestInterceptor(request ->
                        request.header(AUTHORIZATION, "Bearer " + token)).
                logger(new Slf4jLogger(RaidoStableV1Api.class)).
                logLevel(Logger.Level.FULL).
                target(RaidoStableV1Api.class, props.getRaidoServerUrl());
    }

    public RaidoStableV1Api basicRaidStableClient() {
        return basicRaidStableClient(operatorToken);
    }
}