package au.org.raid.api.dto;

import au.org.raid.api.dto.legacy.*;
import lombok.Data;

import java.util.List;

@Data
public class LegacyRaid {
    private LegacyIdentifier id;
    private LegacyDates dates;
    private LegacyAccess access;
    private List<LegacyTitle> titles;
    private List<LegacyDescription> descriptions;
    private List<LegacyAlternateUrl> alternateUrls;
    private String metadataSchema;
}
