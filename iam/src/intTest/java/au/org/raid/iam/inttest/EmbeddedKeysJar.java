package au.org.raid.iam.inttest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.keycloak.util.JsonSerialization.writeValueAsString;

/**
 * Writes a copy of raid-iam.jar whose bundled federation harvest key set holds the given test keys.
 * The shipped set is empty until real Registration Authority keys exist, so this is the only way to
 * prove the embedded-key fallback end to end. Keys are generated per run; nothing is committed.
 */
public final class EmbeddedKeysJar {

    static final String RESOURCE = "au/org/raid/iam/provider/federation/harvest-jwks.json";

    private EmbeddedKeysJar() {
    }

    public static Path write(final Path source, final Path target, final SigningKey... keys) {
        try {
            Files.createDirectories(target.getParent());
            final var jwks = writeValueAsString(Map.of("keys", Arrays.stream(keys).map(SigningKey::jwk).toList()))
                    .getBytes(StandardCharsets.UTF_8);
            var replaced = false;
            try (var in = new JarFile(source.toFile());
                 var out = new JarOutputStream(Files.newOutputStream(target))) {
                for (final var entries = in.entries(); entries.hasMoreElements(); ) {
                    final var entry = entries.nextElement();
                    out.putNextEntry(new JarEntry(entry.getName()));
                    if (entry.getName().equals(RESOURCE)) {
                        out.write(jwks);
                        replaced = true;
                    } else if (!entry.isDirectory()) {
                        try (var stream = in.getInputStream(entry)) {
                            stream.transferTo(out);
                        }
                    }
                    out.closeEntry();
                }
            }
            if (!replaced) {
                throw new IllegalStateException(RESOURCE + " not found in " + source);
            }
            return target;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
