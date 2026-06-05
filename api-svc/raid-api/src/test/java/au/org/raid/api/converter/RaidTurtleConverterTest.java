package au.org.raid.api.converter;

import au.org.raid.api.service.rdf.RaidRdfService;
import au.org.raid.idl.raidv2.model.Id;
import au.org.raid.idl.raidv2.model.Owner;
import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.RegistrationAgency;
import au.org.raid.idl.raidv2.model.Title;
import au.org.raid.idl.raidv2.model.TitleType;
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

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaidTurtleConverterTest {
    @Mock
    private RaidRdfService raidRdfService;
    
    @InjectMocks
    private RaidTurtleConverter converter;
    
    @Test
    @DisplayName("Should return true for canWrite when media type is text/turtle and class is RaidDto")
    void canWriteReturnsTrueWhenMediaTypeIsTurtleAndClassIsRaidDto() {
        // Given
        var mediaType = new MediaType("text", "turtle");
        
        // When
        var result = converter.canWrite(RaidDto.class, mediaType);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("Should return false for canWrite when media type is not text/turtle")
    void canWriteReturnsFalseWhenMediaTypeIsNotTurtle() {
        // Given
        var mediaType = new MediaType("application", "json");
        
        // When
        var result = converter.canWrite(RaidDto.class, mediaType);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("Should write RaidDto as Turtle")
    void writeRaidDtoAsTurtle() throws Exception {
        // Given
        var raidDto = createSampleRaidDto();
        HttpOutputMessage outputMessage = new MockHttpOutputMessage();
        Model model = ModelFactory.createDefaultModel();
        model.createResource("https://raid.org/123.456/abc")
            .addProperty(model.createProperty("http://example.org/property"), "test");
        
        when(raidRdfService.toRdfModel(any(RaidDto.class))).thenReturn(model);
        
        // When
        converter.write(raidDto, MediaType.parseMediaType("text/turtle"), outputMessage);
        
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
        id.setSchemaUri("https://raid.org");
        
        var registrationAgency = new RegistrationAgency();
        registrationAgency.setId("https://ror.org/02stey378");
        registrationAgency.setSchemaUri("https://ror.org");
        id.setRegistrationAgency(registrationAgency);
        
        var owner = new Owner();
        owner.setId("https://ror.org/02stey378");
        owner.setSchemaUri("https://ror.org");
        owner.setServicePoint(20000003L);
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
        titleType.setId("https://vocabulary.raid.org/title.type.schema/5");
        titleType.setSchemaUri("https://vocabulary.raid.org/title.type.schema/376");
        title.setType(titleType);
        
        titles.add(title);
        raidDto.setTitle(titles);
        
        return raidDto;
    }
}