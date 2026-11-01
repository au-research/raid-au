package au.org.raid.orcidupdater.deletion;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ResearcherDeletionController {
    private final ResearcherDeletionService researcherDeletionService;

    /**
     * Contract operation deleteResearcherData. The admin role is checked by SecurityConfig;
     * errors are mapped by ProblemExceptionHandler.
     */
    @PostMapping("/admin/researcher-deletions")
    public ResearcherDeletionResponse deleteResearcherData(@Valid @RequestBody final ResearcherDeletionRequest request) {
        return researcherDeletionService.delete(request);
    }
}
