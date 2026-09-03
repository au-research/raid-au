package au.org.raid.api.service.rrid;

import au.org.raid.api.validator.AbstractUriValidator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;

@Getter
@RequiredArgsConstructor
public class RridService extends AbstractUriValidator {
    // Source prefix (e.g. AB, SCR, CVCL, IMSR) then '_' then the id, which may itself contain a
    // colon (e.g. RRID:IMSR_JAX:000664). Restricted to the observed RRID charset rather than \S+
    // so that stored ids can't carry braces/quotes/query-fragments into the resolver URL, DataCite
    // metadata, or the static site.
    public final String regex = "^https://scicrunch\\.org/resolver/RRID:[A-Za-z0-9]+_[A-Za-z0-9:._-]+$";
    private final RestTemplate restTemplate;

    /**
     * The bare scicrunch.org/resolver/ URL is served behind a Cloudflare interactive challenge
     * (HTTP 403 to every non-browser client), so a server-side HEAD of the stored URL can never
     * confirm existence. The ".json" resolver variant is not challenged and returns a clean
     * HTTP 200 for a known RRID and HTTP 404 for an unknown one, so we existence-check that
     * instead. The stored id and the DataCite relatedIdentifier value remain the bare URL.
     */
    @Override
    protected String resolverUri(final String uri) {
        return uri + ".json";
    }

    @Override
    protected String resolverName() {
        return "RRID";
    }
}
