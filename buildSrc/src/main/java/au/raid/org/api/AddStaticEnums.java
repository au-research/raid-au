/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package au.raid.org.api;


import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static au.raid.org.api.Utils.*;

/**
 * Adds enums to JSON Schema file according to the dynamic enumeration specification from the LinkML data model.
 *
 * @author jkesanie
 */
public abstract class AddStaticEnums extends DefaultTask {

    @Input
    abstract ListProperty<File> getLinkMLEnumsFiles();

    @Input
    abstract ListProperty<File> getEnumInfoFiles();

    @Input
    abstract Property<File> getGeneratedSchemaFile();

    @Input
    abstract Property<File> getOutputFile();


    private Set<Mapping> preprocessEnumInfo(List<SchemaMapping> enuminfos) {
        // enumInfo required a little pre-processing
        final Set<Mapping> mappings = new HashSet<Mapping>();
        enuminfos.forEach(sm -> {
            mappings.add(sm);
            if(sm.values() != null) {
                mappings.add(sm.values());
            }
        });
        return mappings;
    }
    private void validateEnumInfo(@NotNull Map<String, DynamicEnum> enums, @NotNull Set<Mapping> mappings) {
        Set<String> enumIds = mappings.stream().map(Mapping::enumID).collect(Collectors.toSet());
        enums.keySet().forEach(e -> {
            if(!enumIds.contains(e)) {
                throw new RuntimeException("Dynamic enum %s missing from enum info file.".formatted(e));
            }
        });
    }

    @TaskAction
    void generated() throws Exception {
        Map<String, DynamicEnum> enums = new HashMap<String, DynamicEnum>();
        for(File enumFileProp : getLinkMLEnumsFiles().get()) {
            enums.putAll(loadDynamicEnums(enumFileProp));
        }
        List<SchemaMapping> enumInfos = new ArrayList<SchemaMapping>();
        for(File enumInfoProp : getEnumInfoFiles().get()) {
            enumInfos.addAll(loadEnumInfo(enumInfoProp));
        }

        Set<Mapping> mappings = preprocessEnumInfo(enumInfos);
        validateEnumInfo(enums, mappings);

        Map<String, Object> jsonSchema = Utils.getJsonTree(getGeneratedSchemaFile().get());
        Map<String, Object> defs = (Map<String, Object>) jsonSchema.get("$defs");
        for(Mapping mapping : mappings) {
            if(!defs.containsKey(mapping.enumID())) {
                throw new RuntimeException("Could not find element $.defs.%s from jsonschema file".formatted(mapping.enumID()));
            }
            Map<String, Object> jsonObj = (Map<String, Object>) defs.get(mapping.enumID());
            List<String> values = Utils.queryValues(enums.get(mapping.enumID()), mapping.source(), false);
            jsonObj.put("enum", values);
        }
        Utils.writeJsonTree(getOutputFile().get(), jsonSchema);


    }

}
