package au.org.raid.api.controller;

import au.org.raid.api.dto.BackfillResult;
import au.org.raid.api.service.RaidMetadataBackfillService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {
    final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    @Mock
    RaidMetadataBackfillService raidMetadataBackfillService;
    @InjectMocks
    AdminController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("POST /admin/backfill-metadata returns 200 with backfill counts")
    void backfillMetadataReturnsResult() throws Exception {
        final var result = BackfillResult.builder()
                .total(10)
                .backfilled(7)
                .skipped(3)
                .build();

        when(raidMetadataBackfillService.backfill()).thenReturn(result);

        mockMvc.perform(post("/admin/backfill-metadata")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.backfilled").value(7))
                .andExpect(jsonPath("$.skipped").value(3));

        verify(raidMetadataBackfillService).backfill();
    }
}
