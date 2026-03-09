package au.org.raid.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BackfillResult {
    private int total;
    private int backfilled;
    private int skipped;
}
