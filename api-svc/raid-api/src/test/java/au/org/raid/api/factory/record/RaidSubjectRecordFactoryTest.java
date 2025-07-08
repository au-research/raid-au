package au.org.raid.api.factory.record;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RaidSubjectRecordFactoryTest {
    private final RaidSubjectRecordFactory factory = new RaidSubjectRecordFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var handle = "_handle";
        final var subjectTypeId = 3;

        final var result = factory.create(handle, subjectTypeId);

        assertThat(result.getHandle(), is(handle));
        assertThat(result.getSubjectTypeId(), is(subjectTypeId));
    }
}