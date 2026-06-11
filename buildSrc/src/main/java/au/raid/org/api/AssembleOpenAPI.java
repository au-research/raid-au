/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package au.raid.org.api;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the final OpenAPI file by combining OpenAPI template and json schema file.
 *
 * @author jkesanie
 */
public abstract class AssembleOpenAPI extends DefaultTask {
    private static final Logger LOG = LoggerFactory.getLogger(AssembleOpenAPI.class);

    @Input
    abstract Property<File> getOpenAPITemplateFile();
    
    @Input
    abstract ListProperty<File> getGeneratedSchemaFiles();

    @Input
    abstract Property<File> getOutputFile();
    
    @TaskAction
    void assemble() throws Exception {
        Map<String, Object> apiTemplate = Utils.getYamlTree(getOpenAPITemplateFile().get());
        Map<String, Object> components = (Map<String, Object>) apiTemplate.get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");

        for(File schemaFile : getGeneratedSchemaFiles().get()) {
            Map<String, Object> schema = Utils.getJsonTree(schemaFile);
            schemas.putAll((Map<String,Object>) schema.get("$defs"));
        }
        String output = new YAMLMapper().writeValueAsString(apiTemplate);
        // hacky stuff
        String outputModified = output.replaceAll("#/\\$defs/", "#/components/schemas/");
        FileUtils.write(getOutputFile().get(), outputModified);

    }

}
