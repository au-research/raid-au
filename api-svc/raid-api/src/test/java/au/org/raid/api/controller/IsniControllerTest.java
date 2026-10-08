package au.org.raid.api.controller;

import au.org.raid.api.client.contributor.isni.IsniClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IsniControllerTest {
    final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    @Mock
    IsniClient isniClient;
    @InjectMocks
    IsniController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("GET /isni/{isni}/name returns 200 with the resolved name")
    void getNameReturnsResolvedName() throws Exception {
        when(isniClient.getName("0000000078519858")).thenReturn("Taylor Swift");

        mockMvc.perform(get("/isni/0000000078519858/name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Taylor Swift"));
    }

    @Test
    @DisplayName("GET /isni/{isni}/name returns 404 when the name can't be resolved")
    void getNameReturnsNotFoundWhenUnresolvable() throws Exception {
        // Checksum-valid (passes isniValidator), but not a real/assigned ISNI.
        when(isniClient.getName("0000000000000001")).thenThrow(new RuntimeException("ISNI not found 0000000000000001"));

        mockMvc.perform(get("/isni/0000000000000001/name"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /isni/{isni}/name returns 400 for a malformed ISNI, without calling the client")
    void getNameReturnsBadRequestForMalformedIsni() throws Exception {
        mockMvc.perform(get("/isni/not-a-real-isni/name"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(isniClient);
    }

    @Test
    @DisplayName("GET /isni/{isni}/name returns 400 for a value crafted to inject into the outbound query")
    void getNameReturnsBadRequestForInjectionAttempt() throws Exception {
        mockMvc.perform(get("/isni/0000000078519858%22+OR+(1=1)/name"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(isniClient);
    }
}
