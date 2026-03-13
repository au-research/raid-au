package au.org.raid.api.client.ror;

import au.org.raid.api.config.properties.RorClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class RorRequestEntityFactory {
    private final RorClientProperties properties;

    public RequestEntity<Void> create(final String ror) {
        final var id = ror.substring(ror.lastIndexOf("/") + 1);

//get(properties.getBaseUrl() + "/organizations/" + id)

        return RequestEntity.method(HttpMethod.GET, URI.create(properties.getBaseUrl() + "organizations/" + id))
                .header("Client-Id", properties.getClientId())
                .build();
    }
}
