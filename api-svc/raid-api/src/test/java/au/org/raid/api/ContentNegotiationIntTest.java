package au.org.raid.api;

import au.org.raid.api.service.RaidHistoryService;
import au.org.raid.api.service.raid.RaidService;
import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.Title;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class ContentNegotiationIntTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RaidService raidService;

    @MockBean
    private RaidHistoryService raidHistoryService;

    /**
     * Test that the API can return Turtle format when requested.
     */
    @Test
    @WithMockUser(roles = {"service-point-user"})
    public void testTurtleFormat() throws Exception {
        // Create mock RAID response
        RaidDto mockRaid = createMockRaid();
        when(raidService.findByHandle(anyString())).thenReturn(Optional.of(mockRaid));

        // Make request with Accept: text/turtle
        MvcResult result = mockMvc.perform(get("/raid/10378.1/1696639")
                        .accept("text/turtle"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/turtle"))
                .andReturn();

        // Check that the response contains the right content
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("https://raid.org.au/10378.1/1696639");
    }

    /**
     * Test that the API can return N-Triples format when requested.
     */
    @Test
    @WithMockUser(roles = {"service-point-user"})
    public void testNTriplesFormat() throws Exception {
        // Create mock RAID response
        RaidDto mockRaid = createMockRaid();
        when(raidService.findByHandle(anyString())).thenReturn(Optional.of(mockRaid));

        // Make request with Accept: application/n-triples
        MvcResult result = mockMvc.perform(get("/raid/10378.1/1696639")
                        .accept("application/n-triples"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/n-triples"))
                .andReturn();

        // Check that the response contains the right content
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("https://raid.org.au/10378.1/1696639");
    }

    /**
     * Test that the API can return RDF/XML format when requested.
     */
    @Test
    @WithMockUser(roles = {"service-point-user"})
    public void testRdfXmlFormat() throws Exception {
        // Create mock RAID response
        RaidDto mockRaid = createMockRaid();
        when(raidService.findByHandle(anyString())).thenReturn(Optional.of(mockRaid));

        // Make request with Accept: application/rdf+xml
        MvcResult result = mockMvc.perform(get("/raid/10378.1/1696639")
                        .accept("application/rdf+xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/rdf+xml"))
                .andReturn();

        // Check that the response contains the right content
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("https://raid.org.au/10378.1/1696639");
    }

    /**
     * Test that the API defaults to JSON when no specific Accept header is provided.
     */
    @Test
    @WithMockUser(roles = {"service-point-user"})
    public void testDefaultJsonFormat() throws Exception {
        // Create mock RAID response
        RaidDto mockRaid = createMockRaid();
        when(raidService.findByHandle(anyString())).thenReturn(Optional.of(mockRaid));

        // Make request with no specific Accept header
        MvcResult result = mockMvc.perform(get("/raid/10378.1/1696639")
                        .accept(MediaType.ALL_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        // Check that the response contains the right content
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("https://raid.org.au/10378.1/1696639");
    }

    private RaidDto createMockRaid() {
        RaidDto raid = new RaidDto();
        
        // Set identifier
        au.org.raid.idl.raidv2.model.Id id = new au.org.raid.idl.raidv2.model.Id();
        id.setId("https://raid.org.au/10378.1/1696639");
        raid.setIdentifier(id);
        
        // Set some titles
        ArrayList<Title> titles = new ArrayList<>();
        Title title = new Title();
        title.setText("Test Raid");
        titles.add(title);
        raid.setTitle(titles);
        
        return raid;
    }
}