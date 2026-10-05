package au.org.raid.api.validator;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Set;

/**
 * Identifies a related object by the exact {@code (schemaUri, id)} pair, as strings. RAID-935:
 * a pair already stored on the version a client is editing, or already confirmed against its
 * resolver, is not sent to the resolver again.
 * <p>
 * The match is deliberately exact: no case folding, no http/https or trailing-slash
 * normalisation. A difference costs one extra resolver call and never skips a check wrongly.
 * The schemaUri is part of the key because the DOI regex also accepts {@code web.archive.org}
 * links, so an id alone is not unique to a scheme.
 */
public record RelatedObjectKey(String schemaUri, String id) {

    /**
     * Reads {@code relatedObject[*].schemaUri} and {@code .id} from a raid's JSON. Works on the
     * raw {@link JsonNode} rather than {@code RaidDto} because
     * {@code RelatedObjectSchemaUriEnum.fromValue} throws on a value it does not know, which a
     * stored record may legitimately hold. Entries missing either field (or holding a non-text
     * value) are skipped.
     *
     * @return the keys found, never null
     */
    public static Set<RelatedObjectKey> extractFrom(final JsonNode raid) {
        final var keys = new HashSet<RelatedObjectKey>();

        if (raid == null) {
            return keys;
        }

        final var relatedObjects = raid.path("relatedObject");
        if (!relatedObjects.isArray()) {
            return keys;
        }

        for (final var relatedObject : relatedObjects) {
            final var schemaUri = relatedObject.path("schemaUri");
            final var id = relatedObject.path("id");

            if (schemaUri.isTextual() && id.isTextual()) {
                keys.add(new RelatedObjectKey(schemaUri.asText(), id.asText()));
            }
        }

        return keys;
    }
}
