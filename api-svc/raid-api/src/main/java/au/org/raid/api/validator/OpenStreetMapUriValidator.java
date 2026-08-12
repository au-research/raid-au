package au.org.raid.api.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;
@Getter
@RequiredArgsConstructor
public class OpenStreetMapUriValidator extends AbstractUriValidator {
    private final RestTemplate restTemplate;
    private final String regex = "^https://(www\\.)?openstreetmap.org/.*$";

    @Override
    protected String resolverName() {
        return "OpenStreetMap";
    }
}
