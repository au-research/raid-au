package au.raid.org.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.jena.atlas.logging.Log;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);


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

    public static List<String> queryValues(Utils.DynamicEnum de, String source, boolean includePrefLabel) throws Exception {
        String query
                = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
                + "select ?prefLabel ?uri "
                + "where { ?uri a skos:Concept . <%s> %s ?uri . ?uri skos:prefLabel ?prefLabel}"
                .formatted(de.source_nodes().get(0), de.relationship_types().get(0));

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(de.source_ontology());

            request.setHeader("Accept", "application/json");
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("query", query));
            String form = URLEncodedUtils.format(params, "UTF-8");
            StringEntity entity = new StringEntity(form, ContentType.APPLICATION_FORM_URLENCODED);
            request.setEntity(entity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String content = EntityUtils.toString(response.getEntity());
                List<String> values = Utils.parseQueryResults(content, source, includePrefLabel);
                Collections.sort(values);
                return values;
            }

        } catch (Exception e) {
            throw e;
        }

    }

    private static List<String> parseQueryResults(String json, String source, boolean includePrefLabel) throws Exception {
        Map<String, Object> data = om.readValue(json, Map.class);
        Map<String, Object> results = (Map<String, Object>) data.get("results");
        List<Map<String, Object>> bindings = (List<Map<String, Object>>) results.get("bindings");
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
