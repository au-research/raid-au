package au.org.raid.orcidupdater.putcode;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.invitation.InvitationRepository;
import au.org.raid.orcidupdater.invitation.InvitationRequest;
import au.org.raid.orcidupdater.orcid.OrcidEntryRepository;
import au.org.raid.orcidupdater.putcode.PutCodeImportResponse.Rejection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Imports put codes for entries a previous updater wrote with the same ORCID client (RAID-1007,
 * Decision 2026-10-23 Q1), so RAiD AU's updater updates those entries instead of duplicating them.
 * The file is read, never fetched: exporting it from the central updater is a RAID-978 step.
 *
 * <p>Each row is imported in its own transaction under the iD's advisory lock, so it can't race a
 * write for the same researcher, and a large file never holds many locks at once. Nothing is
 * written to ORCID: the put codes are used when the researcher next authenticates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PutCodeImportService {
    static final List<String> HEADER = List.of("handle", "orcidId", "putCode");
    private static final Pattern HANDLE = Pattern.compile(InvitationRequest.HANDLE);
    private static final Pattern BARE_ORCID_ID = Pattern.compile("^\\d{4}-\\d{4}-\\d{4}-\\d{3}[\\dX]$");
    private static final Pattern ORCID_ID = Pattern.compile(InvitationRequest.ORCID_ID);
    private static final Pattern PUT_CODE = Pattern.compile("^[1-9]\\d{0,17}$");

    private final OrcidEntryRepository entryRepository;
    private final InvitationRepository invitationRepository;
    private final TransactionTemplate transactionTemplate;
    private final OrcidUpdaterProperties properties;
    private final Clock clock;

    private record Row(String handle, String orcidId, long putCode) {
    }

    public PutCodeImportResponse importCsv(final String csv) {
        if (csv == null || csv.isBlank()) {
            throw new InvalidImportFileException("The file is empty.");
        }
        final var lines = csv.replaceFirst("^﻿", "").split("\\r?\\n", -1);
        var lineNumber = 0;
        while (lineNumber < lines.length && lines[lineNumber].isBlank()) {
            lineNumber++;
        }
        if (!HEADER.equals(fields(lines[lineNumber]))) {
            throw new InvalidImportFileException("The first row must be the header " + String.join(",", HEADER) + ".");
        }

        var imported = 0;
        var skipped = 0;
        final var rejected = new ArrayList<Rejection>();
        final var at = clock.instant();
        for (lineNumber++; lineNumber < lines.length; lineNumber++) {
            if (lines[lineNumber].isBlank()) {
                continue;
            }
            final var line = lineNumber + 1;
            final var fields = fields(lines[lineNumber]);
            final var reason = invalid(fields);
            if (reason != null) {
                rejected.add(new Rejection(line, reason));
                continue;
            }
            final var row = new Row(fields.get(0), orcidIdUrl(fields.get(1)), Long.parseLong(fields.get(2)));
            final boolean held = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                invitationRepository.lockOrcidId(row.orcidId());
                return entryRepository.importPutCode(row.orcidId(), row.handle(), row.putCode(), at);
            }));
            if (held) {
                imported++;
            } else {
                skipped++;
            }
        }
        // counts only: the rows name researchers
        log.info("Put code import: {} imported, {} skipped, {} rejected", imported, skipped, rejected.size());
        return new PutCodeImportResponse(imported, skipped, rejected);
    }

    /** Splits on commas, trimming each field and any double quotes around it. Fields never contain commas. */
    private static List<String> fields(final String line) {
        final var fields = new ArrayList<String>();
        for (final var field : line.split(",", -1)) {
            final var trimmed = field.trim();
            fields.add(trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")
                    ? trimmed.substring(1, trimmed.length() - 1).trim() : trimmed);
        }
        return fields;
    }

    /** @return why the row can't be imported, without echoing it, or null when it can */
    private String invalid(final List<String> fields) {
        if (fields.size() != HEADER.size()) {
            return "expected " + HEADER.size() + " fields, found " + fields.size();
        }
        if (!HANDLE.matcher(fields.get(0)).matches()) {
            return "handle " + InvitationRequest.HANDLE_MESSAGE;
        }
        final var orcidId = fields.get(1);
        if (!BARE_ORCID_ID.matcher(orcidId).matches() && !ORCID_ID.matcher(orcidId).matches()) {
            return "orcidId must be an ORCID iD, bare or as a URL";
        }
        if (!orcidIdUrl(orcidId).startsWith(orcidBaseUrl() + "/")) {
            // e.g. a sandbox export imported into production
            return "orcidId is not an iD on " + orcidBaseUrl();
        }
        if (!PUT_CODE.matcher(fields.get(2)).matches()) {
            return "putCode must be a positive whole number";
        }
        return null;
    }

    /** The iD as a URL, as on the RAiD and in orcid_entry; the central updater keeps it bare. */
    private String orcidIdUrl(final String orcidId) {
        return BARE_ORCID_ID.matcher(orcidId).matches() ? orcidBaseUrl() + "/" + orcidId : orcidId;
    }

    private String orcidBaseUrl() {
        return properties.getOrcid().getBaseUrl().replaceAll("/+$", "");
    }
}
