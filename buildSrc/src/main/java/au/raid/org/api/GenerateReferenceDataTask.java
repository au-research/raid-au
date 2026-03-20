package au.raid.org.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.provider.Property;
import org.yaml.snakeyaml.Yaml;

/**
 * Creates flyway migration file that contains up-to-date vocabulary related data.
 *
 * @author jkesanie
 */public abstract class GenerateReferenceDataTask extends DefaultTask {

    private static final Logger LOG = LoggerFactory.getLogger(GenerateReferenceDataTask.class);

    final private Yaml yaml = new Yaml();

    @Input
    abstract Property<String> getDataModelPath();

    @Input
    abstract Property<String> getOutputFile();

    @Input
    abstract Property<String> getMappingFile();

    @Input
    abstract Property<Integer> getSchemaID();
    
    @Input
    abstract Property<String> getExamplesDir();

    final private ObjectMapper om = new ObjectMapper();

    @TaskAction
    void generate() throws Exception {
        List<Utils.SchemaMapping> enum2Table = Utils.loadMappings(getMappingFile().get());
        Map<String, Utils.DynamicEnum> enums = Utils.loadDynamicEnums(getDataModelPath().get());
        StringBuilder sb = new StringBuilder();
        try {
            for (Utils.SchemaMapping schemaMapping : enum2Table) {
                LOG.info("Processing enum " + schemaMapping.enumID());
                System.out.println("Processing enum " + schemaMapping.enumID());
                sb.append(handleMapping(schemaMapping, "(id, uri, status)", "(%d, '%s', 'active')", enums));
                generateExamples(schemaMapping, enums);
                LOG.info("Done");
                if (schemaMapping.values() != null) {
                    LOG.info("Processing enum " + schemaMapping.values().enumID());
                    System.out.println("Processing enum " + schemaMapping.values().enumID());
                    sb.append(handleMapping(schemaMapping.values(), "(schema_id, uri)", "(%d, '%s')", enums));
                    generateExamples(schemaMapping.values(), enums);
                    LOG.info("Done");
                }
                sb.append("\n\n");
            }
            FileUtils.write(new File(getOutputFile().get()), sb, Charset.forName("UTF-8"));
        }catch(Exception e) {
            LOG.error(e.getMessage(), e);
            LOG.error("Reference data generation failed");
        }         
    }

    private void generateExamples(Mapping m, Map<String, Utils.DynamicEnum> enums) throws Exception {
        Utils.DynamicEnum de = enums.get(m.enumID());
        if (de == null) {            
            throw new Exception("Missing mapped enum " + m.enumID());
        }
        List<String> values = Utils.queryValues(de, m.source(), true);
        File outputFile = new File(getExamplesDir().get(), m.enumID() + "-allowed-values.yaml");
        // hacky way to get rid of the last newline
        for(int i = 0; i < values.size(); i++) {
            FileUtils.write(outputFile, values.get(i).trim(), (i != 0));
            if(i < values.size() - 1) {
                FileUtils.write(outputFile, "\n", true);    
            }
        }
    }
    
    private String handleMapping(Mapping m, String columns, String valuesTemplate, Map<String, Utils.DynamicEnum> enums) throws Exception {
        StringBuilder sql = new StringBuilder();
        Utils.DynamicEnum de = enums.get(m.enumID());
        if (de == null) {            
            throw new Exception("Missing mapped enum " + m.enumID());
        }
        List<String> values = Utils.queryValues(de, m.source(), false);
        sql.append("insert into api_svc.%s %s ".formatted(m.table(), columns));
        sql.append("values \n");
        sql.append(getValueInserts(values, valuesTemplate));
        sql.append(";\n\n");
        return sql.toString();
    }

    private String getValueInserts(List<String> values, String template) {
        List<String> rows = new ArrayList<String>();
        values.forEach((t) -> {
            rows.add(template.formatted(getSchemaID().get(), t));
        });
        return String.join(",\n", rows);
    }
}
