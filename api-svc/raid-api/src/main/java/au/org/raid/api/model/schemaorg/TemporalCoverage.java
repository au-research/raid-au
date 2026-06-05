package au.org.raid.api.model.schemaorg;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TemporalCoverage {
    private String startDate;
    private String endDate;
}
