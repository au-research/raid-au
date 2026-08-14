package au.org.raid.api.service.doi;

import au.org.raid.api.validator.AbstractUriValidator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;

@Getter
@RequiredArgsConstructor
public class DoiService extends AbstractUriValidator {
    // Both doi.org and dx.doi.org are valid DOI proxy hosts (RAID-798). We accept either as
    // submitted without normalising, consistent with the Handle (RAID-786) and ARK (RAID-793)
    // decisions to store the exact form supplied.
    public final String regex = "^https?://((dx\\.)?doi\\.org/10\\..+|web\\.archive\\.org/.*)";
    private final RestTemplate restTemplate;

    @Override
    protected String resolverName() {
        return "DOI";
    }
}
