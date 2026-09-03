package au.org.raid.api.config;

import au.org.raid.api.converter.RaidNTriplesConverter;
import au.org.raid.api.converter.RaidRdfXmlConverter;
import au.org.raid.api.converter.RaidTurtleConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
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

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(raidTurtleConverter);
        converters.add(raidNTriplesConverter);
        converters.add(raidRdfXmlConverter);
    }
}