package au.org.raid.api.repository;

import au.org.raid.api.endpoint.Constant;
import au.org.raid.db.jooq.api_svc.tables.records.RaidRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static au.org.raid.db.jooq.api_svc.tables.Raid.RAID;
import static au.org.raid.db.jooq.api_svc.tables.ServicePoint.SERVICE_POINT;

@Repository
@RequiredArgsConstructor
public class RaidRepository {
    private final DSLContext dslContext;

    public void insert(final RaidRecord raid) {
        dslContext.insertInto(RAID)
                .set(RAID.HANDLE, raid.getHandle())
                .set(RAID.SERVICE_POINT_ID, raid.getServicePointId())
                .set(RAID.URL, raid.getUrl())
                .set(RAID.URL_INDEX, raid.getUrlIndex())
                .set(RAID.PRIMARY_TITLE, raid.getPrimaryTitle())
                .set(RAID.METADATA, raid.getMetadata())
                .set(RAID.METADATA_SCHEMA, raid.getMetadataSchema())
                .set(RAID.START_DATE, raid.getStartDate())
                .set(RAID.DATE_CREATED, LocalDateTime.now())
                .set(RAID.CONFIDENTIAL, raid.getConfidential())
                .set(RAID.VERSION, raid.getVersion())
                .execute();
    }

    public int update(final RaidRecord raidRecord) {
        return dslContext.update(RAID)
                .set(RAID.PRIMARY_TITLE, raidRecord.getPrimaryTitle())
                .set(RAID.METADATA, raidRecord.getMetadata())
                .set(RAID.METADATA_SCHEMA, raidRecord.getMetadataSchema())
                .set(RAID.START_DATE, raidRecord.getStartDate())
                .set(RAID.CONFIDENTIAL, raidRecord.getConfidential())
                .where(RAID.HANDLE.eq(raidRecord.getHandle()))
                .execute();
    }

    public int updateByHandleAndVersion(final RaidRecord raidRecord) {
        return dslContext.update(RAID)
                .set(RAID.PRIMARY_TITLE, raidRecord.getPrimaryTitle())
                .set(RAID.METADATA, raidRecord.getMetadata())
                .set(RAID.METADATA_SCHEMA, raidRecord.getMetadataSchema())
                .set(RAID.START_DATE, raidRecord.getStartDate())
                .set(RAID.CONFIDENTIAL, raidRecord.getConfidential())
                .set(RAID.VERSION, raidRecord.getVersion() + 1)
                .where(RAID.HANDLE.eq(raidRecord.getHandle()))
                .and(RAID.VERSION.eq(raidRecord.getVersion()))
                .execute();
    }

    public Optional<RaidRecord> findByHandle(final String handle) {
        return dslContext.select(RAID.fields())
                .from(RAID)
                .where(RAID.HANDLE.eq(handle)).
                fetchOptional(record -> new RaidRecord()
                        .setHandle(RAID.HANDLE.getValue(record))
                        .setServicePointId(RAID.SERVICE_POINT_ID.getValue(record))
                        .setUrl(RAID.URL.getValue(record))
                        .setUrlIndex(RAID.URL_INDEX.getValue(record))
                        .setMetadataSchema(RAID.METADATA_SCHEMA.getValue(record))
                        .setMetadata(RAID.METADATA.getValue(record))
                        .setDateCreated(RAID.DATE_CREATED.getValue(record))
                        .setStartDate(RAID.START_DATE.getValue(record))
                        .setConfidential(RAID.CONFIDENTIAL.getValue(record))
                        .setPrimaryTitle(RAID.PRIMARY_TITLE.getValue(record))
                );
    }

    public Optional<RaidRecord> findByHandleAndVersion(final String handle, final int version) {
        return dslContext.select(RAID.fields())
                .from(RAID)
                .where(RAID.HANDLE.eq(handle)
                        .and(RAID.VERSION.eq(version))).
                fetchOptional(record -> new RaidRecord()
                        .setVersion(RAID.VERSION.getValue(record))
                        .setHandle(RAID.HANDLE.getValue(record))
                        .setServicePointId(RAID.SERVICE_POINT_ID.getValue(record))
                        .setUrl(RAID.URL.getValue(record))
                        .setUrlIndex(RAID.URL_INDEX.getValue(record))
                        .setMetadataSchema(RAID.METADATA_SCHEMA.getValue(record))
                        .setMetadata(RAID.METADATA.getValue(record))
                        .setDateCreated(RAID.DATE_CREATED.getValue(record))
                        .setStartDate(RAID.START_DATE.getValue(record))
                        .setConfidential(RAID.CONFIDENTIAL.getValue(record))
                        .setPrimaryTitle(RAID.PRIMARY_TITLE.getValue(record))
                );
    }

    public List<RaidRecord> findAllByServicePointId(final Long servicePointId) {
        return dslContext.select(RAID.fields()).
                select(SERVICE_POINT.fields()).
                from(RAID).join(SERVICE_POINT).onKey().
                where(
                        RAID.SERVICE_POINT_ID.eq(servicePointId)
                ).
                orderBy(RAID.DATE_CREATED.desc()).
                limit(Constant.MAX_EXPERIMENTAL_RECORDS).
                fetch(record -> new RaidRecord()
                        .setVersion(RAID.VERSION.getValue(record))
                        .setHandle(RAID.HANDLE.getValue(record))
                        .setServicePointId(RAID.SERVICE_POINT_ID.getValue(record))
                        .setUrl(RAID.URL.getValue(record))
                        .setUrlIndex(RAID.URL_INDEX.getValue(record))
                        .setMetadataSchema(RAID.METADATA_SCHEMA.getValue(record))
                        .setMetadata(RAID.METADATA.getValue(record))
                        .setDateCreated(RAID.DATE_CREATED.getValue(record))
                        .setStartDate(RAID.START_DATE.getValue(record))
                        .setConfidential(RAID.CONFIDENTIAL.getValue(record))
                        .setPrimaryTitle(RAID.PRIMARY_TITLE.getValue(record))
                );
    }

    public List<RaidRecord> findAllByServicePointOrNotConfidentialId(Long servicePointId) {
        return dslContext.select(RAID.fields())
                .from(RAID)
                .where(
                        RAID.SERVICE_POINT_ID.eq(servicePointId).or(RAID.CONFIDENTIAL.equal(false))
                )
                .orderBy(RAID.DATE_CREATED.desc())
                .limit(Constant.MAX_EXPERIMENTAL_RECORDS)
                .fetch(record -> new RaidRecord()
                        .setVersion(RAID.VERSION.getValue(record))
                        .setHandle(RAID.HANDLE.getValue(record))
                        .setServicePointId(RAID.SERVICE_POINT_ID.getValue(record))
                        .setUrl(RAID.URL.getValue(record))
                        .setUrlIndex(RAID.URL_INDEX.getValue(record))
                        .setMetadataSchema(RAID.METADATA_SCHEMA.getValue(record))
                        .setMetadata(RAID.METADATA.getValue(record))
                        .setDateCreated(RAID.DATE_CREATED.getValue(record))
                        .setStartDate(RAID.START_DATE.getValue(record))
                        .setConfidential(RAID.CONFIDENTIAL.getValue(record))
                        .setPrimaryTitle(RAID.PRIMARY_TITLE.getValue(record))
                );
    }
}