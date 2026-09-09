package au.org.raid.api.config.servicepoint;

import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The register of Registration Agency Service Point ID block allocations.
 *
 * <p>Loaded straight from the classpath rather than bound as Spring configuration
 * so that no environment variable can reassign an agency's block: allocation is
 * the Registration Authority's to make, and travels with the artefact.
 */
@Component
public class RegistrationAgencyRegister {
    static final String DEFAULT_RESOURCE_PATH = "registration-agencies.yaml";
    private static final String ROR_PREFIX = "https://ror.org/";

    private final long blockSize;
    private final List<RegistrationAgency> agencies;

    public RegistrationAgencyRegister() {
        this(new ClassPathResource(DEFAULT_RESOURCE_PATH));
    }

    RegistrationAgencyRegister(final Resource resource) {
        final var contents = read(resource);
        this.blockSize = blockSize(contents);
        this.agencies = agencies(contents);
        validate();
    }

    /**
     * The first Service Point ID of the block allocated to the agency identified
     * by {@code ror}.
     *
     * @throws RegistrationAgencyNotRegisteredException if the ROR is missing or unallocated
     */
    public long servicePointIdStart(final String ror) {
        if (ror == null || ror.isBlank()) {
            throw new RegistrationAgencyNotRegisteredException(
                    "raid.identifier.registration-agency-identifier is not set. Set it to the ROR of " +
                            "the Registration Agency operating this instance. Its Service Point ID range " +
                            "is derived from that ROR, and is assigned by the RAiD Registration Authority.");
        }

        return find(ror)
                .map(this::startOf)
                .orElseThrow(() -> new RegistrationAgencyNotRegisteredException(
                        "Registration Agency '" + ror + "' has no Service Point ID range allocated. " +
                                "Contact the RAiD Registration Authority to be allocated one, which is then " +
                                "recorded in " + DEFAULT_RESOURCE_PATH + " and released. This instance cannot " +
                                "start until then, because minting without an allocated range would produce " +
                                "Service Point IDs that collide with another agency's."));
    }

    public Optional<RegistrationAgency> find(final String ror) {
        return agencies.stream()
                .filter(agency -> !agency.reserved())
                .filter(agency -> agency.ror().equals(ror))
                .findFirst();
    }

    public long blockSize() {
        return blockSize;
    }

    public List<RegistrationAgency> agencies() {
        return List.copyOf(agencies);
    }

    long startOf(final RegistrationAgency agency) {
        return agency.block() * blockSize;
    }

    private Map<String, Object> read(final Resource resource) {
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Registration Agency register not found at " + resource.getDescription());
        }

        final var factory = new YamlMapFactoryBean();
        factory.setResources(resource);
        factory.afterPropertiesSet();

        final var contents = factory.getObject();

        if (contents == null || contents.isEmpty()) {
            throw new IllegalStateException(
                    "Registration Agency register at " + resource.getDescription() + " is empty");
        }

        return contents;
    }

    private long blockSize(final Map<String, Object> contents) {
        if (!(contents.get("blockSize") instanceof Number size)) {
            throw new IllegalStateException("Registration Agency register has no blockSize");
        }

        return size.longValue();
    }

    @SuppressWarnings("unchecked")
    private List<RegistrationAgency> agencies(final Map<String, Object> contents) {
        if (!(contents.get("agencies") instanceof List<?> entries) || entries.isEmpty()) {
            throw new IllegalStateException("Registration Agency register lists no agencies");
        }

        final var parsed = new ArrayList<RegistrationAgency>();

        for (final var entry : entries) {
            final var fields = (Map<String, Object>) entry;

            parsed.add(new RegistrationAgency(
                    (String) fields.get("name"),
                    (String) fields.get("instance"),
                    (String) fields.get("ror"),
                    fields.get("block") instanceof Number block ? block.intValue() : 0,
                    Boolean.TRUE.equals(fields.get("reserved"))
            ));
        }

        return parsed;
    }

    /**
     * Guards the invariants an onboarding pull request is most likely to break.
     * A duplicated block or ROR would reintroduce exactly the collision this
     * register exists to prevent, so it must fail the build rather than a deploy.
     */
    private void validate() {
        if (blockSize <= 0) {
            throw new IllegalStateException("Registration Agency register blockSize must be positive");
        }

        final Set<Integer> blocks = new HashSet<>();
        final Set<String> rors = new HashSet<>();

        for (final var agency : agencies) {
            if (agency.name() == null || agency.name().isBlank()) {
                throw new IllegalStateException("Registration Agency register has an entry with no name");
            }

            if (agency.block() <= 0) {
                throw new IllegalStateException(
                        "Registration Agency '" + agency.name() + "' has no positive block");
            }

            if (!blocks.add(agency.block())) {
                throw new IllegalStateException(
                        "Block " + agency.block() + " is allocated more than once, which would let two " +
                                "Registration Agencies mint colliding Service Point IDs");
            }

            if (agency.reserved()) {
                if (agency.ror() != null) {
                    throw new IllegalStateException(
                            "Reserved block " + agency.block() + " must not carry a ROR");
                }
                continue;
            }

            if (agency.ror() == null || !agency.ror().startsWith(ROR_PREFIX)) {
                throw new IllegalStateException(
                        "Registration Agency '" + agency.name() + "' must have a ROR beginning " + ROR_PREFIX);
            }

            if (!rors.add(agency.ror())) {
                throw new IllegalStateException(
                        "ROR " + agency.ror() + " appears more than once in the Registration Agency register");
            }
        }
    }
}
