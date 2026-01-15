package au.org.raid.api.repository;

import au.org.raid.api.security.EncryptionConverter;
import au.org.raid.db.jooq.tables.records.ServicePointRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static au.org.raid.db.jooq.tables.ServicePoint.SERVICE_POINT;

@Repository
@RequiredArgsConstructor
public class ServicePointRepository {
    private final DSLContext dslContext;
    private final EncryptionConverter encryptionConverter;

    public Optional<ServicePointRecord> findById(final long servicePointId) {
        return dslContext.selectFrom(SERVICE_POINT)
                .where(SERVICE_POINT.ID.eq(servicePointId))
                .fetchOptional(this::fetch);
    }

    public ServicePointRecord create(final ServicePointRecord record) {
        return dslContext.insertInto(SERVICE_POINT)
                .set(SERVICE_POINT.NAME, record.getName())
                .set(SERVICE_POINT.ADMIN_EMAIL, record.getAdminEmail())
                .set(SERVICE_POINT.ENABLED, record.getEnabled())
                .set(SERVICE_POINT.APP_WRITES_ENABLED, record.getAppWritesEnabled())
                .set(SERVICE_POINT.TECH_EMAIL, record.getTechEmail())
                .set(SERVICE_POINT.IDENTIFIER_OWNER, record.getIdentifierOwner())
                .set(SERVICE_POINT.SEARCH_CONTENT, record.getSearchContent())
                .set(SERVICE_POINT.REPOSITORY_ID, record.getRepositoryId())
                .set(SERVICE_POINT.PREFIX, record.getPrefix())
                .set(SERVICE_POINT.PASSWORD, encryptionConverter.to(record.getPassword()))
                .set(SERVICE_POINT.GROUP_ID, record.getGroupId())
                .returning()
                .fetchOne();
    }

    public ServicePointRecord update(final ServicePointRecord record) {
        return dslContext.update(SERVICE_POINT)
                .set(SERVICE_POINT.NAME, record.getName())
                .set(SERVICE_POINT.ADMIN_EMAIL, record.getAdminEmail())
                .set(SERVICE_POINT.ENABLED, record.getEnabled())
                .set(SERVICE_POINT.APP_WRITES_ENABLED, record.getAppWritesEnabled())
                .set(SERVICE_POINT.TECH_EMAIL, record.getTechEmail())
                .set(SERVICE_POINT.IDENTIFIER_OWNER, record.getIdentifierOwner())
                .set(SERVICE_POINT.SEARCH_CONTENT, record.getSearchContent())
                .set(SERVICE_POINT.GROUP_ID, record.getGroupId())
                .where(SERVICE_POINT.ID.eq(record.getId()))
                .returning()
                .fetchOne();
    }

    public List<ServicePointRecord> findAll() {
        return dslContext.selectFrom(SERVICE_POINT)
                .fetch();
    }

    public Optional<ServicePointRecord> findByGroupId(final String groupId) {
        return dslContext.selectFrom(SERVICE_POINT)
                .where(SERVICE_POINT.GROUP_ID.eq(groupId))
                .fetchOptional(this::fetch);
    }

    private ServicePointRecord fetch(ServicePointRecord r) {
        String password = null;
        if (SERVICE_POINT.PASSWORD.getValue(r) != null) {
            password = encryptionConverter.from(Objects.requireNonNull(SERVICE_POINT.PASSWORD.getValue(r)));
        }

        return new ServicePointRecord()
                .setId(SERVICE_POINT.ID.getValue(r))
                .setName(SERVICE_POINT.NAME.getValue(r))
                .setAdminEmail(SERVICE_POINT.ADMIN_EMAIL.getValue(r))
                .setEnabled(SERVICE_POINT.ENABLED.getValue(r))
                .setAppWritesEnabled(SERVICE_POINT.APP_WRITES_ENABLED.getValue(r))
                .setTechEmail(SERVICE_POINT.TECH_EMAIL.getValue(r))
                .setIdentifierOwner(SERVICE_POINT.IDENTIFIER_OWNER.getValue(r))
                .setSearchContent(SERVICE_POINT.SEARCH_CONTENT.getValue(r))
                .setRepositoryId(SERVICE_POINT.REPOSITORY_ID.getValue(r))
                .setPrefix(SERVICE_POINT.PREFIX.getValue(r))
                .setPassword(password)
                .setGroupId(SERVICE_POINT.GROUP_ID.getValue(r));
    }
}