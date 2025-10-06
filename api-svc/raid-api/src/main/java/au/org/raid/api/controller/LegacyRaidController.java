package au.org.raid.api.controller;

import au.org.raid.api.dto.LegacyRaid;
import au.org.raid.api.service.LegacyRaidService;
import au.org.raid.idl.raidv2.model.RaidDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/legacy")
@RequiredArgsConstructor
public class LegacyRaidController {
    private final LegacyRaidService legacyRaidService;

    @GetMapping
    public List<LegacyRaid> listLegacyRaids() {
        return legacyRaidService.findAll();
    }

    @PostMapping
    public RaidDto upgrade(@RequestBody final LegacyRaid legacyRaid) {
        return legacyRaidService.upgrade(legacyRaid);
    }
}
