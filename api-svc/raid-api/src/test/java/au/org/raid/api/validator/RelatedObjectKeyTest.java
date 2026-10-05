package au.org.raid.api.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

class RelatedObjectKeyTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Set<RelatedObjectKey> extract(final String json) throws Exception {
        return RelatedObjectKey.extractFrom(objectMapper.readTree(json));
    }

    @Test
    @DisplayName("Reads schemaUri and id from each related object")
    void readsKeys() throws Exception {
        final var keys = extract("""
                {"relatedObject":[
                  {"id":"https://doi.org/10.1/a","schemaUri":"https://doi.org/","type":{"id":"x"}},
                  {"id":"https://hdl.handle.net/1/b","schemaUri":"https://hdl.handle.net/"}
                ]}""");

        assertThat(keys, equalTo(Set.of(
                new RelatedObjectKey("https://doi.org/", "https://doi.org/10.1/a"),
                new RelatedObjectKey("https://hdl.handle.net/", "https://hdl.handle.net/1/b"))));
    }

    @Test
    @DisplayName("Skips entries missing the id or the schemaUri")
    void skipsEntriesMissingFields() throws Exception {
        final var keys = extract("""
                {"relatedObject":[
                  {"schemaUri":"https://doi.org/"},
                  {"id":"https://doi.org/10.1/a"},
                  {},
                  {"id":"https://doi.org/10.1/ok","schemaUri":"https://doi.org/"}
                ]}""");

        assertThat(keys, equalTo(Set.of(new RelatedObjectKey("https://doi.org/", "https://doi.org/10.1/ok"))));
    }

    @Test
    @DisplayName("Skips entries whose id or schemaUri is null or not text")
    void skipsNonTextValues() throws Exception {
        final var keys = extract("""
                {"relatedObject":[
                  {"id":null,"schemaUri":"https://doi.org/"},
                  {"id":"https://doi.org/10.1/a","schemaUri":null},
                  {"id":5,"schemaUri":"https://doi.org/"}
                ]}""");

        assertThat(keys, empty());
    }

    @Test
    @DisplayName("Keeps a schemaUri that RelatedObjectSchemaUriEnum does not know")
    void keepsUnknownSchemaUri() throws Exception {
        final var keys = extract("""
                {"relatedObject":[{"id":"https://old.example/1","schemaUri":"https://old.example/"}]}""");

        assertThat(keys, equalTo(Set.of(new RelatedObjectKey("https://old.example/", "https://old.example/1"))));
    }

    @Test
    @DisplayName("Returns an empty set when relatedObject is absent, not an array, or the json is null")
    void emptyWhenNoRelatedObjects() throws Exception {
        assertThat(extract("{}"), empty());
        assertThat(extract("{\"relatedObject\":null}"), empty());
        assertThat(extract("{\"relatedObject\":\"x\"}"), empty());
        assertThat(extract("{\"relatedObject\":[]}"), empty());
        assertThat(RelatedObjectKey.extractFrom(null), empty());
    }

    @Test
    @DisplayName("Matching is exact: case and trailing slash differences are different keys")
    void matchingIsExact() {
        assertThat(new RelatedObjectKey("https://doi.org/", "https://doi.org/10.1/A")
                .equals(new RelatedObjectKey("https://doi.org/", "https://doi.org/10.1/a")), equalTo(false));
        assertThat(new RelatedObjectKey("https://doi.org/", "https://doi.org/10.1/a/")
                .equals(new RelatedObjectKey("https://doi.org/", "https://doi.org/10.1/a")), equalTo(false));
    }
}
