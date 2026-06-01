package au.org.raid.db.jooq;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiSvcSchemaTest {

    @Test
    void schemaNameIsDefaultSoJooqUsesConnectionSearchPath() {
        assertEquals("", ApiSvc.API_SVC.getName());
    }
}
