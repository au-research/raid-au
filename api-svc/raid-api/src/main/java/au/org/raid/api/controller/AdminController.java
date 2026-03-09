package au.org.raid.api.controller;

import au.org.raid.api.dto.BackfillResult;
import au.org.raid.api.service.RaidMetadataBackfillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final RaidMetadataBackfillService raidMetadataBackfillService;

    @PostMapping("/backfill-metadata")
    public ResponseEntity<BackfillResult> backfillMetadata() {
        return ResponseEntity.ok(raidMetadataBackfillService.backfill());
    }
}
