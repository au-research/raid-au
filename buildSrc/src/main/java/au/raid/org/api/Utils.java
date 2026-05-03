package au.raid.org.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final long MIN_CALL_SPACING_MS = 500;
    private static final long MAX_BACKOFF_MS = 60_000;
    private static final AtomicLong nextAllowedCallMs = new AtomicLong(0);

    public record DynamicEnum(String source_ontology, java.util.List<String> source_nodes, java.util.List<String> relationship_types, boolean is_direct, boolean traverse_up, boolean include_self) { }
    public record ValueMapping(String enumID, String table, String source) implements Mapping {};
    public record SchemaMapping(String enumID, String table, String source, ValueMapping values) implements Mapping {};

    final private static Yaml yaml = new Yaml();
    final private static ObjectMapper om = new ObjectMapper();

    public static Map<String, Object> getJsonTree(File file) throws Exception {
        return om.readValue(file, Map.class);
    }

    public static void writeJsonTree(File file, Map<String, Object> tree) throws Exception {
        FileUtils.write(file, om.writeValueAsString(tree));
    }
    public static Map<String, Object> getYamlTree(File file) throws Exception {
        InputStream is = new FileInputStream(file);
        Map<String, Object> map = yaml.loadAs(is, Map.class);
        is.close();;
        return map;
    }

    public static List<SchemaMapping> loadEnumInfo(File file) throws Exception {
        return loadMappings(file.getPath());
    }

    public static @NotNull List<SchemaMapping> loadMappings(String filePath) throws Exception {
        InputStream inputStream = new FileInputStream(new File(filePath));
        List<Object> o = om.convertValue(yaml.load(inputStream), List.class);
        List<SchemaMapping> r = new ArrayList<SchemaMapping>();
        for (Object _o : o) {
            r.add(om.convertValue(_o, SchemaMapping.class));
        }
        return r;
    }
    public static @NotNull Map<String, Utils.DynamicEnum> loadDynamicEnums(String filePath) throws Exception {
        return loadDynamicEnums(new File(filePath));
    }


    public static @NotNull Map<String, Utils.DynamicEnum> loadDynamicEnums(File file) throws Exception {
        InputStream i = new FileInputStream(file);
        Map<String, Object> root = yaml.load(i);
        Map<String, Object> obj = (Map<String, Object>) root.get("enums");
        Map<String, Utils.DynamicEnum> r = new HashMap<String, DynamicEnum>();
        for (String enumID : obj.keySet()) {
            Map<String, Object> e = (Map<String, Object>) obj.get(enumID);
            if (e.containsKey("reachable_from")) {
                LOG.info("Loading dynamic enum from " + e.get("reachable_from"));
                Utils.DynamicEnum de = om.convertValue(e.get("reachable_from"), Utils.DynamicEnum.class);
                r.put(enumID, de);

            }
        }
        return r;

    }

    public static List<String> queryValues(File cacheDir, String enumID, Utils.DynamicEnum de, String source, boolean includePrefLabel) throws Exception {
        String json = fetchSparqlJson(cacheDir, enumID, de);
        List<String> values = parseQueryResults(json, source, includePrefLabel);
        Collections.sort(values);
        return values;
    }

    static String buildQuery(Utils.DynamicEnum de) {
        return "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
                + "select ?prefLabel ?uri "
                + "where { ?uri a skos:Concept . <%s> %s ?uri . ?uri skos:prefLabel ?prefLabel}"
                .formatted(de.source_nodes().get(0), de.relationship_types().get(0));
    }

    private static String fetchSparqlJson(File cacheDir, String enumID, Utils.DynamicEnum de) throws Exception {
        String query = buildQuery(de);
        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                throttle();
                String json = postSparqlQuery(de.source_ontology(), query);
                writeCache(cacheDir, enumID, json);
                return json;
            } catch (RateLimitedException e) {
                lastError = e;
                if (attempt == MAX_ATTEMPTS) break;
                long wait = e.retryAfterMs > 0 ? e.retryAfterMs : exponentialBackoffMs(attempt);
                wait = Math.min(wait, MAX_BACKOFF_MS);
                LOG.warn("SPARQL rate-limited for {} (attempt {}/{}), waiting {}ms", enumID, attempt, MAX_ATTEMPTS, wait);
                Thread.sleep(wait);
            } catch (Exception e) {
                lastError = e;
                if (attempt == MAX_ATTEMPTS) break;
                long wait = exponentialBackoffMs(attempt);
                LOG.warn("SPARQL request failed for {} (attempt {}/{}): {} — retrying in {}ms", enumID, attempt, MAX_ATTEMPTS, e.getMessage(), wait);
                Thread.sleep(wait);
            }
        }
        String cached = readCache(cacheDir, enumID);
        if (cached != null) {
            LOG.warn("Falling back to cached SPARQL response for {} after {} failed attempts", enumID, MAX_ATTEMPTS);
            return cached;
        }
        throw new IOException("SPARQL fetch failed for %s after %d attempts and no cache entry at %s".formatted(enumID, MAX_ATTEMPTS, cacheFile(cacheDir, enumID)), lastError);
    }

    private static long exponentialBackoffMs(int attempt) {
        return Math.min(1000L << Math.min(attempt, 5), 30_000L);
    }

    private static void throttle() throws InterruptedException {
        while (true) {
            long now = System.currentTimeMillis();
            long next = nextAllowedCallMs.get();
            if (now >= next) {
                if (nextAllowedCallMs.compareAndSet(next, now + MIN_CALL_SPACING_MS)) return;
            } else {
                Thread.sleep(next - now);
            }
        }
    }

    private static String postSparqlQuery(String endpoint, String query) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(endpoint);
            request.setHeader("Accept", "application/json");
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("query", query));
            String form = URLEncodedUtils.format(params, "UTF-8");
            StringEntity entity = new StringEntity(form, ContentType.APPLICATION_FORM_URLENCODED);
            request.setEntity(entity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                StatusLine status = response.getStatusLine();
                int code = status.getStatusCode();
                String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
                if (code == 429) {
                    throw new RateLimitedException(parseRetryAfterMs(response.getFirstHeader("Retry-After")));
                }
                if (code >= 500 || code < 200) {
                    throw new IOException("SPARQL endpoint returned %d: %s".formatted(code, truncate(body, 200)));
                }
                if (code >= 400) {
                    throw new IOException("SPARQL endpoint returned %d: %s".formatted(code, truncate(body, 200)));
                }
                return body;
            }
        }
    }

    private static long parseRetryAfterMs(Header header) {
        if (header == null) return 0;
        try {
            return Math.max(0, Long.parseLong(header.getValue().trim())) * 1000L;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    static File cacheFile(File cacheDir, String enumID) {
        return new File(cacheDir, enumID + ".json");
    }

    static String readCache(File cacheDir, String enumID) {
        if (cacheDir == null) return null;
        File f = cacheFile(cacheDir, enumID);
        if (!f.isFile()) return null;
        try {
            String json = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
            Map<String, Object> parsed = om.readValue(json, Map.class);
            if (parsed == null || !(parsed.get("results") instanceof Map)) {
                LOG.warn("Cache file {} is not a SPARQL JSON envelope; ignoring", f);
                return null;
            }
            return json;
        } catch (Exception e) {
            LOG.warn("Failed to read cache {}: {}", f, e.getMessage());
            return null;
        }
    }

    private static void writeCache(File cacheDir, String enumID, String json) {
        if (cacheDir == null) return;
        try {
            if (!cacheDir.isDirectory() && !cacheDir.mkdirs()) {
                LOG.warn("Could not create cache dir {}", cacheDir);
                return;
            }
            FileUtils.writeStringToFile(cacheFile(cacheDir, enumID), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Failed to write cache for {}: {}", enumID, e.getMessage());
        }
    }

    private static class RateLimitedException extends IOException {
        final long retryAfterMs;
        RateLimitedException(long retryAfterMs) {
            super("HTTP 429 rate limited" + (retryAfterMs > 0 ? " (retry-after " + retryAfterMs + "ms)" : ""));
            this.retryAfterMs = retryAfterMs;
        }
    }

    static List<String> parseQueryResults(String json, String source, boolean includePrefLabel) throws Exception {
        Map<String, Object> data = om.readValue(json, Map.class);
        Map<String, Object> results = (Map<String, Object>) data.get("results");
        if (results == null) {
            throw new IOException("SPARQL response has no 'results' field: " + truncate(json, 200));
        }
        List<Map<String, Object>> bindings = (List<Map<String, Object>>) results.get("bindings");
        if (bindings == null) {
            throw new IOException("SPARQL response has no 'results.bindings' field: " + truncate(json, 200));
        }
        List<String> r = new ArrayList<String>();
        for (Map<String, Object> b : bindings) {
            Map<String, Object> value = (Map<String, Object>) b.get(source);
            Map<String, Object> label = (Map<String, Object>) b.get("prefLabel");
            String enumValue = (String) value.get("value");
            if(includePrefLabel) {
                enumValue = enumValue + "|" + label.get("value");
            }
            r.add(enumValue);
        }
        return r;
    }

}
