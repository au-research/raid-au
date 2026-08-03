package au.org.raid.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
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

    @Bean
    public RestTemplate restTemplate(
            @Value("${raid.rest-template.connect-timeout:5s}") final Duration connectTimeout,
            @Value("${raid.rest-template.read-timeout:10s}") final Duration readTimeout) {

        // Bound connect/read timeouts so a slow or unreachable external resolver
        // (timeout, DNS failure, connection refused) surfaces as a clean validation
        // failure instead of hanging the request. This is the shared RestTemplate,
        // so the bound also applies to other outbound clients. See RAID-802.
        final var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        RestTemplate restTemplate = new RestTemplate(requestFactory);

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

    public static void main(String[] args) {
        SpringApplication.run(Api.class, args);
    }
}