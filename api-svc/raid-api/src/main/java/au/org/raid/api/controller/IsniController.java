package au.org.raid.api.controller;

import au.org.raid.api.client.contributor.isni.IsniClient;
import au.org.raid.api.dto.IsniNameDto;
import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.api.validator.IsniValidator;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import java.util.List;

// RAID-920 follow-up: ISNI has no public, CORS-enabled API like ORCID's, so
// the frontend can't resolve a contributor's name directly from the browser -
// this wraps the existing IsniClient (used internally for contributor
// validation/DataCite metadata) so the frontend has something to call.
//
// Lives under /ui/** (review feedback, robleney-ardc on #701): this is a
// frontend helper, not part of the RAiD API's public registry contract that
// agencies/integrators build against, so it's kept out of /raid/ and
// /service-point/ and hidden from the published API docs (@Hidden below).
@RestController
@RequestMapping("/ui/isni")
@RequiredArgsConstructor
@Hidden
public class IsniController {
    private final IsniClient isniClient;
    // Stateless/dependency-free, same as its other call site
    // (ContributorValidationConfig) - not worth a Spring bean.
    private final IsniValidator isniValidator = new IsniValidator();

    @GetMapping("/{isni}/name")
    public ResponseEntity<IsniNameDto> getName(@PathVariable final String isni) {
        // This endpoint requires authentication (SecurityConfig's /ui/** matcher) but is
        // still reachable by any logged-in caller, so the raw path variable must never
        // reach IsniRequestEntityFactory unvalidated - it gets substituted directly into
        // the outbound SRU query string with no escaping. Rejecting anything that isn't a
        // well-formed, checksum-valid ISNI here closes that off before it can happen.
        if (!isniValidator.validate(isni)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return ResponseEntity.ok(IsniNameDto.builder().name(isniClient.getName(isni)).build());
        } catch (RestClientException e) {
            // RAID-809 convention: the resolver, not the request, is at fault - a
            // connection/timeout/5xx talking to the ISNI registry, not "no such ISNI".
            throw new ResolverUnavailableException(List.of(
                    ResolverUnavailableException.toUnavailableResolver("isni", isni, "ISNI", e)));
        } catch (RuntimeException e) {
            // getName() throws a plain RuntimeException for both "ISNI not found" and "no
            // name resolvable for this ISNI" - a genuine, clean non-existence, not a
            // resolver outage - so there's nothing to return.
            return ResponseEntity.notFound().build();
        }
    }
}
