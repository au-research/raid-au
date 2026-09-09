package au.org.raid.api.config.servicepoint;

/**
 * One Registration Agency's Service Point ID block allocation, as recorded in
 * registration-agencies.yaml.
 *
 * @param name     the agency's name, used in error messages
 * @param instance the agency's RAiD instance domain, or null for a reserved block
 * @param ror      the agency's ROR, or null for a reserved block
 * @param block    the allocated block index; the block's first Service Point ID is block * blockSize
 * @param reserved true for a block held back rather than allocated to an agency
 */
public record RegistrationAgency(
        String name,
        String instance,
        String ror,
        int block,
        boolean reserved
) {
}
