package au.org.raid.api.factory;

import au.org.raid.db.jooq.tables.records.ServicePointRecord;
import au.org.raid.idl.raidv2.model.PublicServicePoint;
import au.org.raid.idl.raidv2.model.ServicePoint;
import org.springframework.stereotype.Component;

@Component
public class ServicePointFactory {
    public ServicePoint create(final ServicePoint servicePointDto) {
        return new ServicePoint()
                .id(servicePointDto.getId())
                .name(servicePointDto.getName())
                .adminEmail(servicePointDto.getAdminEmail())
                .techEmail(servicePointDto.getTechEmail())
                .repositoryId(servicePointDto.getRepositoryId())
                .prefix(servicePointDto.getPrefix())
                .enabled(servicePointDto.getEnabled())
                .appWritesEnabled(servicePointDto.getAppWritesEnabled())
                .groupId(servicePointDto.getGroupId());
    }

    /**
     * Maps to the public view of a service point. Deliberately copies only id, name and
     * identifierOwner so that no contact details can leak through this path.
     */
    public PublicServicePoint createPublic(final ServicePointRecord servicePointRecord) {
        return new PublicServicePoint()
                .id(servicePointRecord.getId())
                .name(servicePointRecord.getName())
                .identifierOwner(servicePointRecord.getIdentifierOwner());
    }

    public ServicePoint create(final ServicePointRecord servicePointRecord) {
        return new ServicePoint()
                .id(servicePointRecord.getId())
                .name(servicePointRecord.getName())
                .adminEmail(servicePointRecord.getAdminEmail())
                .appWritesEnabled(servicePointRecord.getAppWritesEnabled())
                .enabled(servicePointRecord.getEnabled())
                .identifierOwner(servicePointRecord.getIdentifierOwner())
                .techEmail(servicePointRecord.getTechEmail())
                .repositoryId(servicePointRecord.getRepositoryId())
                .prefix(servicePointRecord.getPrefix())
                .groupId(servicePointRecord.getGroupId());
    }
}
