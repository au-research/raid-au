package au.org.raid.orcidupdater.putcode;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.config.SecurityConfig;
import au.org.raid.orcidupdater.putcode.PutCodeImportResponse.Rejection;
import au.org.raid.orcidupdater.security.ProblemSecurityHandler;
import au.org.raid.orcidupdater.web.ProblemExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PutCodeImportController.class)
@Import({SecurityConfig.class, ProblemSecurityHandler.class, ProblemExceptionHandler.class})
@EnableConfigurationProperties(OrcidUpdaterProperties.class)
@TestPropertySource(properties = {
        "orcid-updater.orcid.client-id=APP-TEST",
        "orcid-updater.orcid.client-secret=test",
        "orcid-updater.orcid.client-name=ARDC RAiD Service",
        "orcid-updater.orcid.base-url=https://sandbox.orcid.org",
        "orcid-updater.orcid.api-url=https://api.sandbox.orcid.org/v3.0",
        "orcid-updater.raid-api.base-url=http://localhost:8080",
        "orcid-updater.keycloak.issuer-uri=http://localhost:8001/realms/raid",
        "orcid-updater.mail.host=localhost",
        "orcid-updater.mail.from=orcid-updater@example.org",
        "orcid-updater.email-hash-secret=test",
        "orcid-updater.token-encryption-key=test-token-encryption-key-32-chars",
        "orcid-updater.link-encryption-key=test-link-encryption-key-32-chars",
        "orcid-updater.api-key=test-api-key",
        "orcid-updater.app-base-url=http://localhost:7080",
        "orcid-updater.contact-email=privacy@example.org"})
@DisplayName("PutCodeImportController")
class PutCodeImportControllerTest {
    private static final String CSV = "handle,orcidId,putCode\n10.82841/86bf1b6c,0000-0002-1825-0097,1234\n";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PutCodeImportService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static SimpleGrantedAuthority admin() {
        return new SimpleGrantedAuthority("ROLE_" + SecurityConfig.ADMIN_ROLE);
    }

    @Test
    @DisplayName("Passes the CSV to the service and returns its report")
    void imports() throws Exception {
        when(service.importCsv(CSV)).thenReturn(new PutCodeImportResponse(1, 2, List.of(new Rejection(4, "putCode must be a positive whole number"))));

        mockMvc.perform(post("/admin/put-code-imports").with(jwt().authorities(admin()))
                        .contentType("text/csv").content(CSV))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"imported": 1, "skipped": 2, "rejected": [{"line": 4, "reason": "putCode must be a positive whole number"}]}
                        """, true));
        verify(service).importCsv(CSV);
    }

    @Test
    @DisplayName("A file without the header row: 400 INVALID_IMPORT_FILE")
    void invalidFile() throws Exception {
        when(service.importCsv(any())).thenThrow(new InvalidImportFileException("The first row must be the header handle,orcidId,putCode."));

        mockMvc.perform(post("/admin/put-code-imports").with(jwt().authorities(admin()))
                        .contentType("text/csv").content("x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IMPORT_FILE"))
                .andExpect(jsonPath("$.detail").value("The first row must be the header handle,orcidId,putCode. Nothing was imported."));
    }

    @Test
    @DisplayName("Not sent as text/csv: 415, and the service isn't called")
    void notCsv() throws Exception {
        mockMvc.perform(post("/admin/put-code-imports").with(jwt().authorities(admin()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Without the admin role: 403, and the service isn't called")
    void notAdmin() throws Exception {
        mockMvc.perform(post("/admin/put-code-imports").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_service-point-user")))
                        .contentType("text/csv").content(CSV))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
