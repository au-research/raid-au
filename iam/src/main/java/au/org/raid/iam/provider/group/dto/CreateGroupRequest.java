package au.org.raid.iam.provider.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class CreateGroupRequest {
    @JsonProperty("name")
    private String name;

    @JsonProperty("path")
    private String path;

    @JsonProperty("attributes")
    private Map<String, List<String>> attributes;

    // Default constructor
    public CreateGroupRequest() {}

    // Constructor with parameters
    public CreateGroupRequest(String name, String path) {
        this.name = name;
        this.path = path;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }
}