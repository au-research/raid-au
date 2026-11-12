package au.org.raid.orcidupdater.putcode;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PutCodeImportController {
    private final PutCodeImportService putCodeImportService;

    /**
     * Contract operation importPutCodes. The admin role is checked by SecurityConfig; errors are
     * mapped by ProblemExceptionHandler. The body is optional here so an empty file gets the
     * import's own error rather than a generic one.
     */
    @PostMapping(path = "/admin/put-code-imports", consumes = "text/csv")
    public PutCodeImportResponse importPutCodes(@RequestBody(required = false) final String csv) {
        return putCodeImportService.importCsv(csv);
    }
}
