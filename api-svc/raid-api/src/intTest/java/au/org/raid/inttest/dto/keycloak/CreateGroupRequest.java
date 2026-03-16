package au.org.raid.inttest.dto.keycloak;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGroupRequest {
    // Getters and Setters
    private String name;
    private String path;

    // Constructors
    public CreateGroupRequest() {}

    public CreateGroupRequest(String name, String path) {
        this.name = name;
        this.path = path;
    }

}
