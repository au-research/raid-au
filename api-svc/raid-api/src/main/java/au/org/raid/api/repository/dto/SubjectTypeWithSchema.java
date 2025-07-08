package au.org.raid.api.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectTypeWithSchema {
    private Integer id;
    private String subjectTypeId;
    private String name;
    private String description;
    private String note;
    private Integer schemaId;
    private String schemaUri;
}
