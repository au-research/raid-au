package au.org.raid.iam.provider.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class CreateGroupResponse {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("attributes")
    private Map<String, List<String>> attributes;

    @JsonProperty("message")
    private String message;

    public CreateGroupResponse() {}

    public CreateGroupResponse(String id, String name, Map<String, List<String>> attributes, String message) {
        this.id = id;
        this.name = name;
        this.attributes = attributes;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}