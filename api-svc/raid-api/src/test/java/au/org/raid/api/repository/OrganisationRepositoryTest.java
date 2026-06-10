package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.OrganisationRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.Organisation.ORGANISATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisationRepositoryTest {
    private static final String PID = "https://ror.org/038sjwq14";
    private static final int SCHEMA_ID = 1;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DSLContext dslContext;
    @InjectMocks
    OrganisationRepository organisationRepository;

    @Test
    @DisplayName("findOrCreate() returns the existing record without inserting")
    void findOrCreateReturnsExistingRecord() {
        final var record = new OrganisationRecord(123, PID, SCHEMA_ID);

        when(dslContext.selectFrom(ORGANISATION)
                .where(any(Condition.class))
                .and(any(Condition.class))
                .fetchOptional())
                .thenReturn(Optional.of(record));

        final var result = organisationRepository.findOrCreate(record);

        assertThat(result, is(record));
        verify(dslContext, never()).insertInto(ORGANISATION);
    }

    @Test
    @DisplayName("findOrCreate() inserts the record when it does not exist")
    void findOrCreateInsertsNewRecord() {
        final var record = new OrganisationRecord(null, PID, SCHEMA_ID);
        final var inserted = new OrganisationRecord(123, PID, SCHEMA_ID);

        when(dslContext.selectFrom(ORGANISATION)
                .where(any(Condition.class))
                .and(any(Condition.class))
                .fetchOptional())
                .thenReturn(Optional.empty());

        when(dslContext.insertInto(ORGANISATION)
                .set(ORGANISATION.PID, PID)
                .set(ORGANISATION.SCHEMA_ID, SCHEMA_ID)
                .onConflict(ORGANISATION.PID, ORGANISATION.SCHEMA_ID)
                .doNothing()
                .returning()
                .fetchOptional())
                .thenReturn(Optional.of(inserted));

        final var result = organisationRepository.findOrCreate(record);

        assertThat(result, is(inserted));
    }

    @Test
    @DisplayName("findOrCreate() re-reads the record when a concurrent insert wins the race")
    void findOrCreateReReadsAfterConflict() {
        final var record = new OrganisationRecord(null, PID, SCHEMA_ID);
        final var existing = new OrganisationRecord(123, PID, SCHEMA_ID);

        // The initial select misses, the conflicted insert returns no rows, and
        // the fallback select (same chain, second invocation) finds the row
        // committed by the competing transaction.
        when(dslContext.selectFrom(ORGANISATION)
                .where(any(Condition.class))
                .and(any(Condition.class))
                .fetchOptional())
                .thenReturn(Optional.empty(), Optional.of(existing));

        when(dslContext.insertInto(ORGANISATION)
                .set(ORGANISATION.PID, PID)
                .set(ORGANISATION.SCHEMA_ID, SCHEMA_ID)
                .onConflict(ORGANISATION.PID, ORGANISATION.SCHEMA_ID)
                .doNothing()
                .returning()
                .fetchOptional())
                .thenReturn(Optional.empty());

        final var result = organisationRepository.findOrCreate(record);

        assertThat(result, is(existing));
    }
}
