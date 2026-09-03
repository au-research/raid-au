package au.org.raid.api.service.handle;

import au.org.raid.api.validator.AbstractUriValidator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;

@Getter
@RequiredArgsConstructor
public class HandleService extends AbstractUriValidator {
    public final String regex = "^https://hdl\\.handle\\.net/\\d+(\\.\\d+)*/\\S+$";
    private final RestTemplate restTemplate;

    @Override
    protected String resolverName() {
        return "Handle";
    }
}
