package au.org.raid.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
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

    public static void main(String[] args) {
        SpringApplication.run(Api.class, args);
    }
}