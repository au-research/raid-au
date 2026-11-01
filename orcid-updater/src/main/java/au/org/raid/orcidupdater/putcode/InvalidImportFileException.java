package au.org.raid.orcidupdater.putcode;

/** The file as a whole can't be imported (empty, or no header row); no row was read. */
public class InvalidImportFileException extends RuntimeException {
    public InvalidImportFileException(final String message) {
        super(message);
    }
}
