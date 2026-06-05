package au.org.raid.api.converter;

import au.org.raid.api.service.rdf.RaidRdfService;
import au.org.raid.idl.raidv2.model.Id;
import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.Title;
import au.org.raid.idl.raidv2.model.TitleType;
import au.org.raid.idl.raidv2.model.Date;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RaidJsonLdConverterTest {

    private RaidJsonLdConverter converter;

    @Mock
    private RaidRdfService raidRdfService;

    @Mock
    private HttpOutputMessage outputMessage;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        converter = new RaidJsonLdConverter(raidRdfService, objectMapper);
    }

    @Test
    void testCanRead() {
        assertFalse(converter.canRead(RaidDto.class, MediaType.valueOf("application/ld+json")));
    }

    @Test
    void testCanWrite() {
        assertTrue(converter.canWrite(RaidDto.class, MediaType.valueOf("application/ld+json")));
        assertFalse(converter.canWrite(RaidDto.class, MediaType.APPLICATION_JSON));
        assertFalse(converter.canWrite(String.class, MediaType.valueOf("application/ld+json")));
    }

    @Test
    void testGetSupportedMediaTypes() {
        assertEquals(MediaType.valueOf("application/ld+json"), converter.getSupportedMediaTypes().get(0));
    }

    @Test
    void testRead() {
        assertThrows(UnsupportedOperationException.class, () -> 
            converter.read(RaidDto.class, null));
    }

    @Test
    void testWrite() throws IOException {
        // Prepare test data
        RaidDto raidDto = createSampleRaidDto();
        
        // Mock the output stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(outputMessage.getBody()).thenReturn(baos);
        HttpHeaders headers = new HttpHeaders();
        when(outputMessage.getHeaders()).thenReturn(headers);
        
        // Call the method to test
        converter.write(raidDto, MediaType.valueOf("application/ld+json"), outputMessage);
        
        // Verify the results
        assertEquals(MediaType.valueOf("application/ld+json"), headers.getContentType());
        assertTrue(baos.size() > 0);
        
        // Verify output was written and contains JSON-LD required fields
        String response = baos.toString();
        assertTrue(baos.size() > 0, "Response should not be empty");
        
        // Parse JSON and verify key elements
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(response);
        
        assertTrue(jsonNode.has("@context"), "Response should have @context property");
        assertTrue(jsonNode.has("@type"), "Response should have @type property");
        assertEquals("ResearchProject", jsonNode.get("@type").asText(), "Type should be ResearchProject");
        assertTrue(jsonNode.has("@id"), "Response should have @id property");
        assertTrue(jsonNode.has("name"), "Response should have name property");
        assertEquals("Sample RAID Title", jsonNode.get("name").asText(), "Name should match the title");
    }
    
    private RaidDto createSampleRaidDto() {
        RaidDto raidDto = new RaidDto();
        
        // Set identifier
        Id id = new Id();
        id.setId("https://raid.org/10.25.1/abc123");
        id.setSchemaUri("https://raid.org");
        raidDto.setIdentifier(id);
        
        // Set title
        Title title = new Title();
        title.setText("Sample RAID Title");
        TitleType titleType = new TitleType();
        titleType.setId("https://vocabulary.raid.org/title.type.schema/5");
        titleType.setSchemaUri("https://vocabulary.raid.org/title.type.schema/376");
        title.setType(titleType);
        raidDto.title(java.util.List.of(title));
        
        // Set date
        Date date = new Date();
        date.setStartDate("2023-01-01");
        raidDto.setDate(date);
        
        return raidDto;
    }
}