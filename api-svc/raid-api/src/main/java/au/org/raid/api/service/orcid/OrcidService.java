package au.org.raid.api.service.orcid;

import au.org.raid.api.validator.AbstractUriValidator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;

@Getter
@RequiredArgsConstructor
public class OrcidService extends AbstractUriValidator {
    private final String regex = "^https://orcid\\.org/[\\d]{4}-[\\d]{4}-[\\d]{4}-[\\d]{3}[\\d|X]{1}$";
    private final RestTemplate restTemplate;

    @Override
    protected String resolverName() {
        return "ORCID";
    }
}
