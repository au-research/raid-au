package au.org.raid.orcidupdater.serviceauth;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * The public half of the updater's signing key. Keycloak reads it to check the updater's token
 * requests (RAID-1011 configures the client with this URL).
 */
@RestController
@RequiredArgsConstructor
public class JwksController {
    public static final String PATH = "/.well-known/jwks.json";

    private final ServiceKeyStore serviceKeyStore;

    @GetMapping(value = PATH, produces = "application/jwk-set+json")
    public Map<String, Object> jwks() {
        return new JWKSet(serviceKeyStore.publicJwk()).toJSONObject(true);
    }
}
