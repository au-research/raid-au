package au.org.raid.api.repository;

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Renders the repository's SQL against a recording connection, so the table and column names are checked. */
class ContributorStatusResetRepositoryTest {
    private static final String ORCID = "https://orcid.org/0000-0001-0000-0001";

    private final List<String> statements = new ArrayList<>();
    private final List<List<Object>> bindings = new ArrayList<>();
    private ContributorStatusResetRepository repository;

    @BeforeEach
    void setUp() {
        final var connection = new MockConnection(context -> {
            statements.add(context.sql());
            bindings.add(List.of(context.bindings()));
            if (context.sql().startsWith("select")) {
                final var dsl = DSL.using(SQLDialect.POSTGRES);
                final var result = dsl.newResult(ContributorStatusResetRepository.PID);
                final var record = dsl.newRecord(ContributorStatusResetRepository.PID);
                record.set(ContributorStatusResetRepository.PID, ORCID);
                result.add(record);
                return new MockResult[]{new MockResult(1, result)};
            }
            return new MockResult[]{new MockResult(1)};
        });
        repository = new ContributorStatusResetRepository(DSL.using(connection, SQLDialect.POSTGRES));
    }

    @Test
    @DisplayName("findPending() selects the iDs of recorded contributors not yet reset, up to the limit")
    void findsPending() {
        assertThat(repository.findPending(10)).containsExactly(ORCID);

        assertThat(statements.get(0))
                .contains("from \"contributor_status_reset\"")
                .contains("\"reset_at\" is null")
                .contains("order by \"contributor_id\"")
                .contains("fetch next ? rows only");
        assertThat(bindings.get(0)).contains(10L);
    }

    @Test
    @DisplayName("markReset() records when that contributor was reset")
    void marksReset() {
        repository.markReset(ORCID);

        assertThat(statements.get(0))
                .startsWith("update \"contributor_status_reset\"")
                .contains("set \"reset_at\" = cast(? as timestamp with time zone)")
                .contains("where \"pid\" = ?");
        assertThat(OffsetDateTime.parse(bindings.get(0).get(0).toString().replace(' ', 'T')))
                .isCloseTo(OffsetDateTime.now(), org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MINUTES));
        assertThat(bindings.get(0).get(1)).isEqualTo(ORCID);
    }
}
