package au.org.raid.api.config;

import au.org.raid.api.converter.RaidNTriplesConverter;
import au.org.raid.api.converter.RaidRdfXmlConverter;
import au.org.raid.api.converter.RaidTurtleConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.HttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WebConfigTest {
    @Mock
    private RaidTurtleConverter raidTurtleConverter;
    
    @Mock
    private RaidNTriplesConverter raidNTriplesConverter;
    
    @Mock
    private RaidRdfXmlConverter raidRdfXmlConverter;
    
    @InjectMocks
    private WebConfig webConfig;
    
    @Test
    @DisplayName("Should add custom converters to the list")
    void shouldAddCustomConvertersToList() {
        // Given
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        
        // When
        webConfig.extendMessageConverters(converters);
        
        // Then
        assertThat(converters).hasSize(3);
        assertThat(converters).contains(raidTurtleConverter, raidNTriplesConverter, raidRdfXmlConverter);
    }
}