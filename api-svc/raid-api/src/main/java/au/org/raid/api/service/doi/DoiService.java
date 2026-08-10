package au.org.raid.api.service.doi;

import au.org.raid.api.validator.AbstractUriValidator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;

@Getter
@RequiredArgsConstructor
public class DoiService extends AbstractUriValidator {
    public final String regex = "^https?://(doi\\.org/10\\..+|web\\.archive\\.org/.*)";
    private final RestTemplate restTemplate;

    @Override
    protected String resolverName() {
        return "DOI";
    }
}
