package au.org.raid.orcidupdater.putcode;

import au.org.raid.orcidupdater.invitation.InvitationRepository;
import au.org.raid.orcidupdater.orcid.OrcidEntryRepository;
import au.org.raid.orcidupdater.putcode.PutCodeImportResponse.Rejection;
import au.org.raid.orcidupdater.support.TestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RAID-1007, with the database stubbed. The test settings use the ORCID sandbox. */
@DisplayName("PutCodeImportService")
class PutCodeImportServiceTest {
    private static final String BARE_ID = "0000-0002-1825-0097";
    private static final String ID = "https://sandbox.orcid.org/" + BARE_ID;
    private static final String HANDLE = "10.82841/86bf1b6c";
    private static final Instant NOW = Instant.parse("2026-10-28T04:00:00Z");

    private final OrcidEntryRepository entryRepository = mock(OrcidEntryRepository.class);
    private final InvitationRepository invitationRepository = mock(InvitationRepository.class);

    private PutCodeImportService service;

    @BeforeEach
    void setUp() {
        service = new PutCodeImportService(entryRepository, invitationRepository,
                new TransactionTemplate(mock(PlatformTransactionManager.class)), TestProperties.complete(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(entryRepository.importPutCode(anyString(), anyString(), anyLong(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("Imports each row under the iD's lock, turning a bare iD into a URL on the configured ORCID")
    void imports() {
        final var response = service.importCsv("""
                handle,orcidId,putCode
                10.82841/86bf1b6c,0000-0002-1825-0097,1234
                10.82841/2f3c4d5e,https://sandbox.orcid.org/0000-0001-5109-3700,5678
                """);

        assertThat(response).isEqualTo(new PutCodeImportResponse(2, 0, List.of()));
        final InOrder order = inOrder(invitationRepository, entryRepository);
        order.verify(invitationRepository).lockOrcidId(ID);
        order.verify(entryRepository).importPutCode(ID, HANDLE, 1234L, NOW);
        order.verify(invitationRepository).lockOrcidId("https://sandbox.orcid.org/0000-0001-5109-3700");
        order.verify(entryRepository).importPutCode("https://sandbox.orcid.org/0000-0001-5109-3700", "10.82841/2f3c4d5e", 5678L, NOW);
    }

    @Test
    @DisplayName("A row whose entry already has a put code is counted as skipped")
    void skipped() {
        when(entryRepository.importPutCode(ID, HANDLE, 1234L, NOW)).thenReturn(false);

        assertThat(service.importCsv("handle,orcidId,putCode\n10.82841/86bf1b6c,0000-0002-1825-0097,1234\n"))
                .isEqualTo(new PutCodeImportResponse(0, 1, List.of()));
    }

    @Test
    @DisplayName("Invalid rows are rejected by line number, without echoing them; valid rows are still imported")
    void rejects() {
        final var response = service.importCsv("""
                handle,orcidId,putCode
                10.82841/86bf1b6c,0000-0002-1825-0097,1234
                not-a-handle,0000-0002-1825-0097,1
                10.82841/a,0000-0002-1825-009,1
                10.82841/b,https://orcid.org/0000-0002-1825-0097,1
                10.82841/c,0000-0002-1825-0097,0
                10.82841/d,0000-0002-1825-0097,12x
                10.82841/e,0000-0002-1825-0097
                """);

        assertThat(response.imported()).isEqualTo(1);
        assertThat(response.rejected()).containsExactly(
                new Rejection(3, "handle must be a RAiD handle as prefix/suffix"),
                new Rejection(4, "orcidId must be an ORCID iD, bare or as a URL"),
                new Rejection(5, "orcidId is not an iD on https://sandbox.orcid.org"),
                new Rejection(6, "putCode must be a positive whole number"),
                new Rejection(7, "putCode must be a positive whole number"),
                new Rejection(8, "expected 3 fields, found 2"));
        assertThat(response.rejected()).extracting(Rejection::reason).noneMatch(reason -> reason.contains("0000-0002"));
        verify(entryRepository).importPutCode(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Tolerates a byte order mark, CRLF line endings, blank lines, spaces and quotes")
    void lenientFormatting() {
        final var response = service.importCsv("﻿\r\n\"handle\", \"orcidId\" ,putCode\r\n\r\n"
                + " \"10.82841/86bf1b6c\" , 0000-0002-1825-0097 ,\"1234\"\r\n");

        assertThat(response).isEqualTo(new PutCodeImportResponse(1, 0, List.of()));
        verify(entryRepository).importPutCode(ID, HANDLE, 1234L, NOW);
    }

    @Test
    @DisplayName("Line numbers count blank lines, so they match the file")
    void lineNumbers() {
        assertThat(service.importCsv("handle,orcidId,putCode\n\n10.82841/x,0000-0002-1825-0097,-1\n").rejected())
                .containsExactly(new Rejection(3, "putCode must be a positive whole number"));
    }

    @Test
    @DisplayName("An empty file, or one without the header row: rejected as a whole, nothing imported")
    void invalidFile() {
        assertThatThrownBy(() -> service.importCsv(null)).isInstanceOf(InvalidImportFileException.class);
        assertThatThrownBy(() -> service.importCsv(" \n")).isInstanceOf(InvalidImportFileException.class);
        assertThatThrownBy(() -> service.importCsv("10.82841/86bf1b6c,0000-0002-1825-0097,1234\n"))
                .isInstanceOf(InvalidImportFileException.class)
                .hasMessage("The first row must be the header handle,orcidId,putCode.");
        assertThatThrownBy(() -> service.importCsv("handle,orcid,putCode\n")).isInstanceOf(InvalidImportFileException.class);
        verifyNoInteractions(entryRepository, invitationRepository);
    }

    @Test
    @DisplayName("A header with no rows imports nothing")
    void headerOnly() {
        assertThat(service.importCsv("handle,orcidId,putCode")).isEqualTo(new PutCodeImportResponse(0, 0, List.of()));
    }
}
