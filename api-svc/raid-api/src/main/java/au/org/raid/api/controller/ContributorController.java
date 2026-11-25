package au.org.raid.api.controller;

import au.org.raid.api.service.ContributorStatusService;
import au.org.raid.idl.raidv2.api.ContributorApi;
import au.org.raid.idl.raidv2.model.ContributorStatusUpdate;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@SecurityScheme(name = "bearerAuth", scheme = "bearer", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
public class ContributorController implements ContributorApi {
    private final ContributorStatusService contributorStatusService;

    @Override
    public ResponseEntity<Void> updateContributorStatus(final ContributorStatusUpdate update) {
        final var found = contributorStatusService.updateStatus(update.getId(), update.getStatus().getValue());

        return found ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
