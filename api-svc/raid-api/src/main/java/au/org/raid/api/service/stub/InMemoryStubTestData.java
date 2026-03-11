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
    public static String NONEXISTENT_TEST_ROR = "https://ror.org/000000042";
    public static String SERVER_ERROR_TEST_ROR = "https://ror.org/000000043";
    public static String NONEXISTENT_TEST_DOI = "https://doi.org/10.42/000000";
    public static String SERVER_ERROR_TEST_DOI = "https://doi.org/10.42/000001";

    public static String NONEXISTENT_TEST_GEONAMES_URI = "https://www.geonames.org/0/not-found.html";
    public static String SERVER_ERROR_TEST_GEONAMES_URI = "https://www.geonames.org/0/server-error.html";

    public static String NONEXISTENT_TEST_OPENSTREETMAP_URI = "https://www.openstreetmap.org/not-found";
    public static String SERVER_ERROR_TEST_OPENSTREETMAP_URI = "https://www.openstreetmap.org/server-error";

    public static String NONEXISTENT_TEST_ISNI = "https://isni.org/isni/0000000000000000";
    public static String SERVER_ERROR_TEST_ISNI = "https://isni.org/isni/0000000000000001";
}
