package au.org.raid.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@OpenAPIDefinition(servers = {@Server(url = "/", description = "Default Server URL")})
@SpringBootApplication
@EnableCaching
@EnableFeignClients
public class Api {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Shared, general-purpose RestTemplate used by outbound clients such as
     * DataCite, the legacy RAiD service and Keycloak logout. Its timeouts are
     * intentionally left unbounded here — those calls (e.g. a DataCite mint) can
     * legitimately run longer than a URI-existence check. URI validators use the
     * separately bounded {@link #uriValidatorRestTemplate} instead. See RAID-802.
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Add JAXB message converter for XML
        Jaxb2RootElementHttpMessageConverter jaxbConverter =
                new Jaxb2RootElementHttpMessageConverter();

        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.add(jaxbConverter);
        // Add other converters as needed
        converters.addAll(restTemplate.getMessageConverters());

        restTemplate.setMessageConverters(converters);
        return restTemplate;
    }

    /**
     * Dedicated RestTemplate for external URI validators (DOI, ROR, GeoNames,
     * OpenStreetMap). Connect/read timeouts are bounded so a slow or unreachable
     * resolver surfaces as a clean validation failure instead of hanging the
     * request, without affecting the shared RestTemplate used for minting and
     * other outbound calls. Configurable via {@code raid.uri-validation.*}.
     * See RAID-802.
     */
    @Bean
    public RestTemplate uriValidatorRestTemplate(
            @Value("${raid.uri-validation.connect-timeout:5s}") final Duration connectTimeout,
            @Value("${raid.uri-validation.read-timeout:10s}") final Duration readTimeout) {

        final var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return new RestTemplate(requestFactory);
    }

    /**
     * RestTemplate for the ARK (arks.org) resolver check (RAID-793). Shares the same bounded
     * connect/read timeouts as {@link #uriValidatorRestTemplate}, but with redirect-following
     * DISABLED - the ARK existence check's signal is which host a 302 redirects to (arks.org
     * itself = unregistered NAAN, a different host = registered NAAN), which is only visible if
     * the redirect isn't transparently followed first.
     */
    @Bean
    public RestTemplate arkResolverRestTemplate(
            @Value("${raid.uri-validation.connect-timeout:5s}") final Duration connectTimeout,
            @Value("${raid.uri-validation.read-timeout:10s}") final Duration readTimeout) {

        // SimpleClientHttpRequestFactory doesn't expose HttpURLConnection#setInstanceFollowRedirects
        // itself (unlike the JVM-wide static HttpURLConnection#setFollowRedirects), so it's
        // overridden here via the connection-preparation hook it does expose.
        final var requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(final java.net.HttpURLConnection connection, final String httpMethod)
                    throws java.io.IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return new RestTemplate(requestFactory);
    }

    public static void main(String[] args) {
        SpringApplication.run(Api.class, args);
    }
}