package au.org.raid.inttest.config;

import tools.jackson.databind.ObjectMapper;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ComponentScan(basePackages = {
        "au.org.raid.inttest"
})
public class IntegrationTestConfig {

    @Bean
    public SpringMvcContract feignContract() {
        return new SpringMvcContract();
    }

//    @Bean
//    public TestClient testClient(final ObjectMapper objectMapper,
//                                 final SpringMvcContract contract,
//                                 @Value("${raid.test.api.url}") final String apiUrl
//    ) {
//        return new TestClient(objectMapper, contract, apiUrl, tokenService);
//    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        final var objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        return objectMapper;
    }

//    @Configuration
//    @ConfigurationProperties(prefix = "raid.test.auth.raid-au", ignoreInvalidFields = true)
//    public static class UserConfig {
//        private String user;
//        private String password;
//
//    }
}