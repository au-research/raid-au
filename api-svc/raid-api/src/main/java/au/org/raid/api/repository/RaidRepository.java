package au.org.raid.api.repository;

import au.org.raid.api.config.properties.ContributorValidationProperties;
import au.org.raid.api.endpoint.Constant;
import au.org.raid.db.jooq.enums.Metaschema;
import au.org.raid.db.jooq.tables.records.RaidRecord;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static au.org.raid.db.jooq.tables.Contributor.CONTRIBUTOR;
import static au.org.raid.db.jooq.tables.Organisation.ORGANISATION;
import static au.org.raid.db.jooq.tables.Raid.RAID;
import static au.org.raid.db.jooq.tables.RaidContributor.RAID_CONTRIBUTOR;
import static au.org.raid.db.jooq.tables.RaidHistory.RAID_HISTORY;
import static au.org.raid.db.jooq.tables.RaidOrganisation.RAID_ORGANISATION;

@Repository
@RequiredArgsConstructor
public class RaidRepository {
    private final ContributorValidationProperties contributorValidationProperties;
//    private static final String ORCID_URI_FORMAT = "https://orcid.org/%s";
    private final DSLContext dslContext;

    public RaidRecord insert(final RaidRecord raid) {
        return dslContext.insertInto(RAID)
                .set(RAID.HANDLE, raid.getHandle())
                .set(RAID.SERVICE_POINT_ID, raid.getServicePointId())
                .set(RAID.URL, raid.getUrl())
                .set(RAID.METADATA, raid.getMetadata())
                .set(RAID.METADATA_SCHEMA, raid.getMetadataSchema())
                .set(RAID.START_DATE, raid.getStartDate())
                .set(RAID.DATE_CREATED, LocalDateTime.now())
                .set(RAID.CONFIDENTIAL, raid.getConfidential())
                .set(RAID.VERSION, raid.getVersion())
                .set(RAID.START_DATE_STRING, raid.getStartDateString())
                .set(RAID.END_DATE, raid.getEndDate())
                .set(RAID.LICENSE, raid.getLicense())
                .set(RAID.ACCESS_TYPE_ID, raid.getAccessTypeId())
                .set(RAID.EMBARGO_EXPIRY, raid.getEmbargoExpiry())
                .set(RAID.ACCESS_STATEMENT, raid.getAccessStatement())
                .set(RAID.ACCESS_STATEMENT_LANGUAGE_ID, raid.getAccessStatementLanguageId())
                .set(RAID.SCHEMA_URI, raid.getSchemaUri())
                .set(RAID.REGISTRATION_AGENCY_ORGANISATION_ID, raid.getRegistrationAgencyOrganisationId())
                .set(RAID.OWNER_ORGANISATION_ID, raid.getOwnerOrganisationId())
                .returning()
                .fetchOne();
    }

    public int update(final RaidRecord record) {
        return dslContext.update(RAID)
                .set(RAID.SERVICE_POINT_ID, record.getServicePointId())
                .set(RAID.METADATA, record.getMetadata())
                .set(RAID.METADATA_SCHEMA, record.getMetadataSchema())
                .set(RAID.START_DATE, record.getStartDate())
                .set(RAID.DATE_CREATED, LocalDateTime.now())
                .set(RAID.CONFIDENTIAL, record.getConfidential())
                .set(RAID.VERSION, record.getVersion())
                .set(RAID.START_DATE_STRING, record.getStartDateString())
                .set(RAID.END_DATE, record.getEndDate())
                .set(RAID.LICENSE, record.getLicense())
                .set(RAID.ACCESS_TYPE_ID, record.getAccessTypeId())
                .set(RAID.EMBARGO_EXPIRY, record.getEmbargoExpiry())
                .set(RAID.ACCESS_STATEMENT, record.getAccessStatement())
                .set(RAID.ACCESS_STATEMENT_LANGUAGE_ID, record.getAccessStatementLanguageId())
                .set(RAID.SCHEMA_URI, record.getSchemaUri())
                .set(RAID.REGISTRATION_AGENCY_ORGANISATION_ID, record.getRegistrationAgencyOrganisationId())
                .set(RAID.OWNER_ORGANISATION_ID, record.getOwnerOrganisationId())
                .where(RAID.HANDLE.eq(record.getHandle()))
                .execute();
    }

    public Optional<RaidRecord> findByHandle(final String handle) {
        return dslContext.selectFrom(RAID)
                .where(RAID.HANDLE.eq(handle))
                .fetchOptional();
    }

    public List<RaidRecord> findAllByServicePointId(final Long servicePointId) {
        return dslContext.selectFrom(RAID)
                .where(RAID.SERVICE_POINT_ID.eq(servicePointId))
                .and(RAID.METADATA_SCHEMA.ne(Metaschema.legacy_metadata_schema_v1))
                .orderBy(RAID.DATE_CREATED.desc())
                .limit(Constant.MAX_EXPERIMENTAL_RECORDS)
                .fetch();
    }

    public List<RaidRecord> findAllByServicePointIdOrHandleIn(final Long servicePointId, List<String> handles) {
        return dslContext.selectFrom(RAID)
                .where(RAID.SERVICE_POINT_ID.eq(servicePointId))
                .or(RAID.HANDLE.in(handles))
                .and(RAID.METADATA_SCHEMA.ne(Metaschema.legacy_metadata_schema_v1))
                .orderBy(RAID.DATE_CREATED.desc())
                .limit(Constant.MAX_EXPERIMENTAL_RECORDS)
                .fetch();
    }

    public List<RaidRecord> findAllByServicePointIdOrNotConfidential(Long servicePointId) {
        return dslContext.selectFrom(RAID)
                .where(
                        RAID.SERVICE_POINT_ID.eq(servicePointId).or(RAID.CONFIDENTIAL.equal(false))
                )
                .and(RAID.METADATA_SCHEMA.ne(Metaschema.legacy_metadata_schema_v1))
                .orderBy(RAID.DATE_CREATED.desc())
                .limit(Constant.MAX_EXPERIMENTAL_RECORDS)
                .fetch();
    }


    public List<RaidRecord> findAll() {
        return dslContext.select()
                .distinctOn(RAID.HANDLE)
                .from(RAID)
                .join(RAID_HISTORY).on(RAID_HISTORY.HANDLE.eq(RAID.HANDLE))
                .and(RAID.METADATA_SCHEMA.notIn(Metaschema.legacy_metadata_schema_v1))
                .fetchInto(RaidRecord.class);
    }

    public List<RaidRecord> findAllByContributorOrcid(final String orcid) {
        return dslContext.select()
                .from(RAID)
                .join(RAID_CONTRIBUTOR)
                .on(RAID.HANDLE.eq(RAID_CONTRIBUTOR.HANDLE))
                .join(CONTRIBUTOR)
                .on(RAID_CONTRIBUTOR.CONTRIBUTOR_ID.eq(CONTRIBUTOR.ID))
                .where(
                        CONTRIBUTOR.PID.eq(contributorValidationProperties.getOrcid().getUrlPrefix().concat(orcid))
                )
                .and(RAID.METADATA_SCHEMA.ne(Metaschema.legacy_metadata_schema_v1))
                .fetchInto(RaidRecord.class);
    }

    public List<RaidRecord> findAllByOrganisationId(final String ror) {
        return dslContext.select()
                .from(RAID)
                .join(RAID_ORGANISATION)
                .on(RAID.HANDLE.eq(RAID_ORGANISATION.HANDLE))
                .join(ORGANISATION)
                .on(RAID_ORGANISATION.ORGANISATION_ID.eq(ORGANISATION.ID))
                .where(
                        ORGANISATION.PID.eq(ror)
                )
                .and(RAID.METADATA_SCHEMA.ne(Metaschema.legacy_metadata_schema_v1))
                .fetchInto(RaidRecord.class);
    }

    public List<RaidRecord> findAllV2() {
        return dslContext.select()
                .distinctOn(RAID.HANDLE)
                .from(RAID)
                .where(RAID.METADATA_SCHEMA.notIn(Metaschema.legacy_metadata_schema_v1)
                )
                .fetchInto(RaidRecord.class);
    }

    public List<RaidRecord> findAllLegacy() {
        return dslContext.select()
                .distinctOn(RAID.HANDLE)
                .from(RAID)
                .where(RAID.METADATA_SCHEMA.in(Metaschema.legacy_metadata_schema_v1)).and(RAID.SERVICE_POINT_ID.eq(20000003L))
                .fetchInto(RaidRecord.class);
    }

    public List<RaidRecord> findAllPublic() {
        return dslContext.select()
                .distinctOn(RAID.HANDLE)
                .from(RAID)
                .join(RAID_HISTORY).on(RAID_HISTORY.HANDLE.eq(RAID.HANDLE))
                .where(RAID.ACCESS_TYPE_ID.in(1, 4)
                        .and(RAID.METADATA_SCHEMA.notIn(Metaschema.legacy_metadata_schema_v1, Metaschema.raido_metadata_schema_v1))
                )
                .fetchInto(RaidRecord.class);
    }

    public List<RaidRecord> findAllNonLegacyRaids() {
        return dslContext.select()
                .distinctOn(RAID.HANDLE)
                .from(RAID)
                .join(RAID_HISTORY).on(RAID_HISTORY.HANDLE.eq(RAID.HANDLE))
                .where(RAID.HANDLE.startsWith("10."))
                .fetchInto(RaidRecord.class);
    }

    public int deleteByHandle(final String handle) {
        return dslContext.delete(RAID).where(RAID.HANDLE.eq(handle)).execute();
    }

    public List<RaidRecord> findAllByServicePointIdAndAccessTypeIdIn(final Long servicePointId,
                                                                     final Integer... accessTypeIds) {
        return dslContext.selectFrom(RAID)
                .where(RAID.SERVICE_POINT_ID.eq(servicePointId))
                .and(RAID.ACCESS_TYPE_ID.in(accessTypeIds))
                .fetchInto(RaidRecord.class);
    }

    public int countByFilters(final Long servicePointId,
                              final LocalDateTime startDate,
                              final LocalDateTime endDate) {
        var condition = DSL.noCondition();

        if (servicePointId != null) {
            condition = condition.and(RAID.SERVICE_POINT_ID.eq(servicePointId));
        }
        if (startDate != null) {
            condition = condition.and(RAID.DATE_CREATED.greaterOrEqual(startDate));
        }
        if (endDate != null) {
            condition = condition.and(RAID.DATE_CREATED.lessThan(endDate));
        }

        return dslContext.selectCount()
                .from(RAID)
                .where(condition)
                .fetchOne(0, int.class);
    }
}