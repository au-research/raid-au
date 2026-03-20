package au.org.raid.api.converter;

import au.org.raid.api.service.rdf.RaidRdfService;
import au.org.raid.idl.raidv2.model.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaidNTriplesConverterTest {
    @Mock
    private RaidRdfService raidRdfService;
    
    @InjectMocks
    private RaidNTriplesConverter converter;
    
    @Test
    @DisplayName("Should return true for canWrite when media type is application/n-triples and class is RaidDto")
    void canWriteReturnsTrueWhenMediaTypeIsNTriplesAndClassIsRaidDto() {
        // Given
        var mediaType = new MediaType("application", "n-triples");
        
        // When
        var result = converter.canWrite(RaidDto.class, mediaType);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("Should return false for canWrite when media type is not application/n-triples")
    void canWriteReturnsFalseWhenMediaTypeIsNotNTriples() {
        // Given
        var mediaType = new MediaType("application", "json");
        
        // When
        var result = converter.canWrite(RaidDto.class, mediaType);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("Should write RaidDto as N-Triples")
    void writeRaidDtoAsNTriples() throws Exception {
        // Given
        var raidDto = createSampleRaidDto();
        HttpOutputMessage outputMessage = new MockHttpOutputMessage();
        Model model = ModelFactory.createDefaultModel();
        model.createResource("https://raid.org/123.456/abc")
            .addProperty(model.createProperty("http://example.org/property"), "test");
        
        when(raidRdfService.toRdfModel(any(RaidDto.class))).thenReturn(model);
        
        // When
        converter.write(raidDto, MediaType.parseMediaType("application/n-triples"), outputMessage);
        
        // Then
        var content = ((MockHttpOutputMessage) outputMessage).getBodyAsString();
        assertThat(content).isNotEmpty();
        assertThat(content).contains("https://raid.org/123.456/abc");
    }
    
    private RaidDto createSampleRaidDto() {
        var raidDto = new RaidDto();
        
        // Set identifier
        var id = new Id();
        id.setId("https://raid.org/123.456/abc");
        id.setSchemaUri(RaidIdentifierSchemaURIEnum.HTTPS_RAID_ORG_);
        
        var registrationAgency = new RegistrationAgency();
        registrationAgency.setId("https://ror.org/02stey378");
        registrationAgency.setSchemaUri(RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_);
        id.setRegistrationAgency(registrationAgency);
        
        var owner = new Owner();
        owner.setId("https://ror.org/02stey378");
        owner.setSchemaUri(RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_);
        owner.setServicePoint(new BigDecimal(20000003L));
        id.setOwner(owner);
        
        id.setLicense("Creative Commons CC-0");
        id.setVersion(1);
        
        raidDto.setIdentifier(id);
        
        // Set date
        var date = new au.org.raid.idl.raidv2.model.Date();
        date.setStartDate("2023-01-01");
        date.setEndDate("2023-12-31");
        raidDto.setDate(date);
        
        // Set title
        var titles = new ArrayList<Title>();
        var title = new Title();
        title.setText("Sample RAID");
        
        var titleType = new TitleType();
        titleType.setId(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_5);
        titleType.setSchemaUri(TitleTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_376);
        title.setType(titleType);
        
        titles.add(title);
        raidDto.setTitle(titles);
        
        return raidDto;
    }
}