package au.org.raid.api.repository;

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.db.jooq.tables.Raid.RAID;
import static org.assertj.core.api.Assertions.assertThat;

/** Renders the repository's SQL against a recording connection, so the queue column name is checked. */
class ContributorStatusRefreshRepositoryTest {
    private final List<String> statements = new ArrayList<>();
    private final List<List<Object>> bindings = new ArrayList<>();
    private ContributorStatusRefreshRepository repository;

    @BeforeEach
    void setUp() {
        final var connection = new MockConnection(context -> {
            statements.add(context.sql());
            bindings.add(List.of(context.bindings()));
            if (context.sql().startsWith("select")) {
                final var dsl = DSL.using(SQLDialect.POSTGRES);
                final var result = dsl.newResult(RAID.HANDLE);
                final var record = dsl.newRecord(RAID.HANDLE);
                record.set(RAID.HANDLE, "10.1/a");
                result.add(record);
                return new MockResult[]{new MockResult(1, result)};
            }
            return new MockResult[]{new MockResult(1)};
        });
        repository = new ContributorStatusRefreshRepository(DSL.using(connection, SQLDialect.POSTGRES));
    }

    @Test
    @DisplayName("findRefreshRequired() selects flagged handles in order, up to the limit")
    void findsFlaggedHandles() {
        assertThat(repository.findRefreshRequired(25)).containsExactly("10.1/a");

        assertThat(statements.get(0))
                .contains("\"contributor_status_refresh_required\" = true")
                .contains("order by \"raid\".\"handle\"")
                .contains("fetch next ? rows only");
        assertThat(bindings.get(0)).contains(25L);
    }

    @Test
    @DisplayName("clearRefreshRequired() clears the flag on that RAiD only")
    void clearsFlag() {
        repository.clearRefreshRequired("10.1/a");

        assertThat(statements.get(0))
                .startsWith("update")
                .contains("set \"contributor_status_refresh_required\" = ?");
        assertThat(bindings.get(0)).containsExactly(false, "10.1/a");
    }
}
