package au.org.raid.api.service.stub;

/**
 * Note that, despite implementing logic to return error values, we're not trying
 * to do "white box" integration testing against complicated mocking logic.
 * The intent behind this logic is just to keep the int-tests passing when run
 * against in-memory stubs, or against the real external service.
 * The intent of the of the int-tests themselves (e.g. InvalidPidTes) is to test
 * against the real service and validate that the production logic
 * does actually work with the real external service.
 * An alternate approach to this would be to mark these kind of int-tests as
 * "disabled" when they're run against in-memory stubs.
 */
public class InMemoryStubTestData {
    /* each of these values was confirmed to not exist as at 2023-05-05 */
    public static String NONEXISTENT_TEST_ORCID =
            "https://orcid.org/0000-0001-0000-0009";
    public static String SERVER_ERROR_TEST_ORCID =
            "https://orcid.org/0000-0002-0448-8774";
    public static String NONEXISTENT_TEST_DOI = "https://doi.org/10.42/000000";
    public static String SERVER_ERROR_TEST_DOI = "https://doi.org/10.42/000001";

    public static String NONEXISTENT_TEST_HANDLE = "https://hdl.handle.net/0.0/not-found";
    public static String SERVER_ERROR_TEST_HANDLE = "https://hdl.handle.net/0.0/server-error";

    public static String NONEXISTENT_TEST_RRID = "https://scicrunch.org/resolver/RRID:AB_0000000";
    public static String SERVER_ERROR_TEST_RRID = "https://scicrunch.org/resolver/RRID:AB_5000000";

    public static String NONEXISTENT_TEST_GEONAMES_URI = "https://www.geonames.org/0/not-found.html";
    public static String SERVER_ERROR_TEST_GEONAMES_URI = "https://www.geonames.org/0/server-error.html";

    public static String NONEXISTENT_TEST_OPENSTREETMAP_URI = "https://www.openstreetmap.org/not-found";
    public static String SERVER_ERROR_TEST_OPENSTREETMAP_URI = "https://www.openstreetmap.org/server-error";

    // Must be checksum-valid (ISO 27729 MOD 11-2) so the RAID-791 local checksum gate lets it
    // through to the stub's existence check - calculated check character for digits
    // "000000000000002" is "8".
    public static String NONEXISTENT_TEST_ISNI = "https://isni.org/isni/0000000000000028";
    // Checksum-valid - calculated check character for digits "000000000000000" is "1".
    public static String SERVER_ERROR_TEST_ISNI = "https://isni.org/isni/0000000000000001";
    // Deliberately checksum-INVALID (calculated check character for digits
    // "000000000000000" is "1", not "0") - used to test the RAID-791 local checksum rejection
    // path, which must reject before ever calling the stub/live resolver.
    public static String MALFORMED_TEST_ISNI = "https://isni.org/isni/0000000000000000";

    public static String NONEXISTENT_TEST_ROR = "https://ror.org/000000000";
    public static String SERVER_ERROR_TEST_ROR = "https://ror.org/000000001";

    public static String NONEXISTENT_TEST_WEB_ARCHIVE =
            "https://web.archive.org/web/20200101000000/https://nonexistent.example.com";
    public static String SERVER_ERROR_TEST_WEB_ARCHIVE =
            "https://web.archive.org/web/20200101000000/https://server-error.example.com";

    // NAAN 99999 is a reserved test NAAN (ARK Alliance spec / draft-kunze-ark-43 s2.3), used here
    // only as a deterministic stub trigger; these constants exercise the in-memory ArkServiceStub
    // and never hit the real arks.org resolver.
    // The "reject" signal is not specific to 99999 (verified live 2026-08-18, RAID-793): genuinely
    // unregistered, non-reserved NAANs self-loop to https://arks.org/.info/ark:..., while reserved
    // arks-managed NAANs (incl. 99999) self-loop to http://arks.org/ark:/... — both keep the host
    // on arks.org, which is what ArkService.isUnregistered() matches on, so both are rejected.
    public static String NONEXISTENT_TEST_ARK = "https://arks.org/ark:/99999/not-found";
    public static String SERVER_ERROR_TEST_ARK = "https://arks.org/ark:/99999/server-error";
}
