package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteRight;
import au.org.raid.idl.raidv2.model.Access;
import au.org.raid.idl.raidv2.model.Id;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DataciteRightFactoryTest {

    private final DataciteRightFactory dataciteRightFactory = new DataciteRightFactory();

    @Test
    public void testCreateWithNonNullValues() {
        Access access = new Access();
        Id id = new Id();
        id.setLicense("Test License");

        DataciteRight result = dataciteRightFactory.create(access, id);

        assertEquals("Test License", result.getRights(), "Rights should match the license");
        assertEquals("Test License", result.getRightsURI(), "Rights URI should match the license");
    }

    @Test
    public void testCreateWithNullAccess() {
        Id id = new Id();
        id.setLicense("Test License");

        DataciteRight result = dataciteRightFactory.create(null, id);

        assertNull(result, "DataciteRight should be null when Access is null");
    }

    @Test
    public void testCreateWithNullId() {
        Access access = new Access();

        DataciteRight result = dataciteRightFactory.create(access, null);

        assertNull(result, "DataciteRight should be null when Id is null");
    }

    @Test
    public void testCreateWithNullLicense() {
        Access access = new Access();
        Id id = new Id(); // License not set, implying null

        DataciteRight result = dataciteRightFactory.create(access, id);

        assertEquals("", result.getRights(), "Rights should be empty string when license is null");
        assertEquals("", result.getRightsURI(), "Rights URI should be empty string when license is null");
    }
}