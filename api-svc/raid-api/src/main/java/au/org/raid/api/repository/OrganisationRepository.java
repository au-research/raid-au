package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.OrganisationRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.Organisation.ORGANISATION;

@Repository
@RequiredArgsConstructor
public class OrganisationRepository {
    private final DSLContext dslContext;
    public OrganisationRecord create(final OrganisationRecord record) {
        return dslContext.insertInto(ORGANISATION)
                .set(ORGANISATION.PID, record.getPid())
                .set(ORGANISATION.SCHEMA_ID, record.getSchemaId())
                .returning()
                .fetchOne();
    }

    public OrganisationRecord findOrCreate(final OrganisationRecord record) {
        final var result = dslContext.selectFrom(ORGANISATION)
                .where(ORGANISATION.PID.eq(record.getPid()))
                .and(ORGANISATION.SCHEMA_ID.eq(record.getSchemaId()))
                .fetchOptional();

        if (result.isPresent()) {
            return result.get();
        }

        // A concurrent transaction may insert the same organisation between the
        // select and the insert, so tolerate the conflict and re-read the row.
        return dslContext.insertInto(ORGANISATION)
                .set(ORGANISATION.PID, record.getPid())
                .set(ORGANISATION.SCHEMA_ID, record.getSchemaId())
                .onConflict(ORGANISATION.PID, ORGANISATION.SCHEMA_ID)
                .doNothing()
                .returning()
                .fetchOptional()
                .orElseGet(() -> dslContext.selectFrom(ORGANISATION)
                        .where(ORGANISATION.PID.eq(record.getPid()))
                        .and(ORGANISATION.SCHEMA_ID.eq(record.getSchemaId()))
                        .fetchOptional()
                        .orElseThrow(() -> new IllegalStateException(
                                "Organisation not found after insert conflict: %s".formatted(record.getPid()))));
    }

    public Optional<OrganisationRecord> findByUriAndSchemaId(final String uri, final int schemaId) {
        return dslContext.selectFrom(ORGANISATION)
                .where(ORGANISATION.PID.eq(uri)).and(ORGANISATION.SCHEMA_ID.eq(schemaId))
                .fetchOptional();
    }

    public Optional<OrganisationRecord> findById(final Integer id) {
        return dslContext.selectFrom(ORGANISATION)
                .where(ORGANISATION.ID.eq(id))
                .fetchOptional();
    }
}
