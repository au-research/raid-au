package au.org.raid.db.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationSchemaReferenceTest {

    private static final Pattern API_SVC_REFERENCE = Pattern.compile("\\bapi_svc\\.");

    @Test
    void noMigrationContainsHardcodedApiSvcSchemaReference() throws Exception {
        var migrationDir = Paths.get(
                getClass().getClassLoader().getResource("db/migration").toURI());

        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.list(migrationDir)) {
            files
                    .filter(p -> p.toString().endsWith(".sql"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            var lines = Files.readAllLines(path);
                            for (int i = 0; i < lines.size(); i++) {
                                String line = lines.get(i);
                                if (line.trim().startsWith("--")) {
                                    continue;
                                }
                                Matcher matcher = API_SVC_REFERENCE.matcher(line);
                                if (matcher.find()) {
                                    violations.add("%s:%d: %s".formatted(
                                            path.getFileName(), i + 1, line.trim()));
                                }
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        assertTrue(violations.isEmpty(),
                "Migrations must not hardcode the api_svc schema — use unqualified names so "
                        + "Flyway routes to the correct schema for branch deployments.\n\n"
                        + "Violations:\n" + String.join("\n", violations));
    }
}
