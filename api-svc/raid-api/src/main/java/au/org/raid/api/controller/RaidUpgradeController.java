package au.org.raid.api.controller;

import au.org.raid.api.dto.LegacyRaid;
import au.org.raid.api.service.RaidUpgradeService;
import au.org.raid.idl.raidv2.model.RaidDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/upgrade")
@RequiredArgsConstructor
public class RaidUpgradeController {
    private final RaidUpgradeService raidUpgradeService;

    @GetMapping
    public List<RaidDto> findAll() {
        return raidUpgradeService.findAll();
    }

    @PostMapping
    public RaidDto upgrade(@RequestBody final RaidDto raidDto) {
        return raidUpgradeService.upgrade(raidDto);
    }
}
