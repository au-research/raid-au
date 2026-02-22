package au.org.raid.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RaidCountDto {
    private long count;
    private Long servicePointId;
    private LocalDate startDate;
    private LocalDate endDate;
}
