package au.org.raid.api.controller;

import au.org.raid.api.client.contributor.isni.IsniClient;
import au.org.raid.api.dto.IsniNameDto;
import au.org.raid.api.validator.IsniValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// RAID-920 follow-up: ISNI has no public, CORS-enabled API like ORCID's, so
// the frontend can't resolve a contributor's name directly from the browser -
// this wraps the existing IsniClient (used internally for contributor
// validation/DataCite metadata) so the frontend has something to call.
@RestController
@RequestMapping("/isni")
@CrossOrigin
@RequiredArgsConstructor
public class IsniController {
    private final IsniClient isniClient;
    // Stateless/dependency-free, same as its other call site
    // (ContributorValidationConfig) - not worth a Spring bean.
    private final IsniValidator isniValidator = new IsniValidator();

    @GetMapping("/{isni}/name")
    public ResponseEntity<IsniNameDto> getName(@PathVariable final String isni) {
        // This endpoint is public/unauthenticated (SecurityConfig.ISNI_API), so the raw
        // path variable must never reach IsniRequestEntityFactory unvalidated - it gets
        // substituted directly into the outbound SRU query string with no escaping.
        // Rejecting anything that isn't a well-formed, checksum-valid ISNI here closes
        // that off before it can happen.
        if (!isniValidator.validate(isni)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return ResponseEntity.ok(IsniNameDto.builder().name(isniClient.getName(isni)).build());
        } catch (RuntimeException e) {
            // getName() throws a plain RuntimeException for both "ISNI not found" and "no
            // name resolvable for this ISNI" - either way, there's nothing to return.
            return ResponseEntity.notFound().build();
        }
    }
}
