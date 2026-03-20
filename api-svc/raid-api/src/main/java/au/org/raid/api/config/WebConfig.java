package au.org.raid.api.config;

import au.org.raid.api.converter.RaidJsonLdConverter;
import au.org.raid.api.converter.RaidNTriplesConverter;
import au.org.raid.api.converter.RaidRdfXmlConverter;
import au.org.raid.api.converter.RaidTurtleConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web configuration to register custom converters
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final RaidTurtleConverter raidTurtleConverter;
    private final RaidNTriplesConverter raidNTriplesConverter;
    private final RaidRdfXmlConverter raidRdfXmlConverter;
    private final RaidJsonLdConverter raidJsonLdConverter;

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Add custom converters to handle content negotiation
        converters.add(raidTurtleConverter);
        converters.add(raidNTriplesConverter);
        converters.add(raidRdfXmlConverter);
        converters.add(raidJsonLdConverter);
    }
    
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("turtle", MediaType.valueOf("text/turtle"))
            .mediaType("ntriples", MediaType.valueOf("application/n-triples"))
            .mediaType("rdfxml", MediaType.valueOf("application/rdf+xml"))
            .mediaType("jsonld", MediaType.valueOf("application/ld+json"))
            .favorParameter(false)
            .ignoreAcceptHeader(false)
            .defaultContentType(MediaType.APPLICATION_JSON);
    }
}