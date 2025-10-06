package au.org.raid.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class RaidCorsConfiguration {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Add all allowed origins or use allowedOriginPatterns for wildcard support
        configuration.setAllowedOrigins(List.of(
                "http://localhost:7080",
                "https://app.test.raid.org.au",
                "https://app.demo.raid.org.au",
                "https://app.stage.raid.org.au",
                "https://app.prod.raid.org.au"
        ));
        // Include all necessary HTTP methods including OPTIONS
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Allow all common headers
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        // Allow cookies if needed
        configuration.setAllowCredentials(true);
        // Set max age for preflight requests cache (in seconds)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}