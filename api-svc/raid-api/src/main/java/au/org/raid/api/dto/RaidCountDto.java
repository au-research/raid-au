package au.org.raid.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RaidCountDto {
    private long count;
    private Long servicePointId;
    private String servicePointName;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<OrganisationCountDto> organisations;
}
