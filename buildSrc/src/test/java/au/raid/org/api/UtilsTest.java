package au.raid.org.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    private static final String SAMPLE_SPARQL_JSON = """
            {
              "head": {"vars": ["prefLabel", "uri"]},
              "results": {
                "bindings": [
                  {"prefLabel": {"type":"literal","value":"RAiD"}, "uri": {"type":"uri","value":"https://raid.org/"}},
                  {"prefLabel": {"type":"literal","value":"DataCite"}, "uri": {"type":"uri","value":"https://datacite.org/"}}
                ]
              }
            }
            """;

    @Test
    void parseQueryResults_urisOnly() throws Exception {
        List<String> values = Utils.parseQueryResults(SAMPLE_SPARQL_JSON, "uri", false);
        assertEquals(List.of("https://raid.org/", "https://datacite.org/"), values);
    }

    @Test
    void parseQueryResults_withPrefLabel() throws Exception {
        List<String> values = Utils.parseQueryResults(SAMPLE_SPARQL_JSON, "uri", true);
        assertEquals(
                List.of("https://raid.org/|RAiD", "https://datacite.org/|DataCite"),
                values);
    }

    @Test
    void parseQueryResults_emptyBindings() throws Exception {
        String json = "{\"head\":{\"vars\":[]},\"results\":{\"bindings\":[]}}";
        List<String> values = Utils.parseQueryResults(json, "uri", false);
        assertTrue(values.isEmpty());
    }

    @Test
    void parseQueryResults_missingResults_throwsIOException() {
        String json = "{\"head\":{\"vars\":[]}}";
        IOException ex = assertThrows(IOException.class,
                () -> Utils.parseQueryResults(json, "uri", false));
        assertTrue(ex.getMessage().contains("results"));
    }

    @Test
    void parseQueryResults_missingBindings_throwsIOException() {
        String json = "{\"head\":{\"vars\":[]},\"results\":{}}";
        IOException ex = assertThrows(IOException.class,
                () -> Utils.parseQueryResults(json, "uri", false));
        assertTrue(ex.getMessage().contains("bindings"));
    }

    @Test
    void buildQuery_substitutesNodeAndRelationship() {
        Utils.DynamicEnum de = new Utils.DynamicEnum(
                "https://example.org/sparql",
                List.of("https://example.org/root"),
                List.of("skos:hasTopConcept"),
                true, false, false);
        String q = Utils.buildQuery(de);
        assertTrue(q.contains("<https://example.org/root>"));
        assertTrue(q.contains("skos:hasTopConcept"));
        assertTrue(q.contains("PREFIX skos:"));
    }

    @Test
    void cacheFile_resolvesExpectedPath(@TempDir Path tmp) {
        File f = Utils.cacheFile(tmp.toFile(), "FooEnum");
        assertEquals("FooEnum.json", f.getName());
        assertEquals(tmp.toFile(), f.getParentFile());
    }

    @Test
    void readCache_returnsNull_whenFileMissing(@TempDir Path tmp) {
        assertNull(Utils.readCache(tmp.toFile(), "Nope"));
    }

    @Test
    void readCache_returnsNull_forOldBareListFormat(@TempDir Path tmp) throws Exception {
        File f = new File(tmp.toFile(), "Legacy.json");
        Files.writeString(f.toPath(), "[ \"https://raid.org/\" ]", StandardCharsets.UTF_8);
        assertNull(Utils.readCache(tmp.toFile(), "Legacy"));
    }

    @Test
    void readCache_returnsJson_forValidEnvelope(@TempDir Path tmp) throws Exception {
        File f = new File(tmp.toFile(), "Good.json");
        Files.writeString(f.toPath(), SAMPLE_SPARQL_JSON, StandardCharsets.UTF_8);
        String cached = Utils.readCache(tmp.toFile(), "Good");
        assertNotNull(cached);
        List<String> values = Utils.parseQueryResults(cached, "uri", false);
        assertEquals(2, values.size());
    }

    @Test
    void readCache_returnsNull_whenCacheDirIsNull() {
        assertNull(Utils.readCache(null, "Anything"));
    }
}
