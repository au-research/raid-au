package au.org.raid.orcidupdater.putcode;

import java.util.List;

/**
 * @param imported rows whose put code is now held
 * @param skipped  rows for an iD and RAiD that already had a put code, left unchanged
 * @param rejected rows that failed validation, by line number; never echoing the row
 */
public record PutCodeImportResponse(int imported, int skipped, List<Rejection> rejected) {

    public record Rejection(int line, String reason) {
    }
}
