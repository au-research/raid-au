package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteDto;
import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataciteDtoFactory {
    private static final String TYPE = "dois";
    private static final String SCHEMA_VERSION = "http://datacite.org/schema/kernel-4";
    private static final String IDENTIFIER_TYPE = "DOI";

    private final DataciteAttributesDtoFactory dataciteAttributesDtoFactory;
    private final DataciteIdentifierFactory dataciteIdentifierFactory;

    @SneakyThrows
    public DataciteDto create(RaidCreateRequest request, String handle) {
        final var dto =  new DataciteDto()
                .setSchemaVersion(SCHEMA_VERSION)
                .setType(TYPE)
                .setAttributes(dataciteAttributesDtoFactory.create(request, handle));

        dto.getDataciteIdentifiers().add(dataciteIdentifierFactory.create(handle, IDENTIFIER_TYPE));

        return dto;
    }

    @SneakyThrows
    public DataciteDto create(RaidUpdateRequest request, String handle) {
        final var dto =  new DataciteDto()
                .setSchemaVersion(SCHEMA_VERSION)
                .setType(TYPE)
                .setAttributes(dataciteAttributesDtoFactory.create(request, handle));

        dto.getDataciteIdentifiers().add(dataciteIdentifierFactory.create(handle, IDENTIFIER_TYPE));

        return dto;
    }

    @SneakyThrows
    public DataciteDto create(RaidDto request, String handle) {
        final var dto =  new DataciteDto()
                .setSchemaVersion(SCHEMA_VERSION)
                .setType(TYPE)
                .setAttributes(dataciteAttributesDtoFactory.create(request, handle));

        dto.getDataciteIdentifiers().add(dataciteIdentifierFactory.create(handle, IDENTIFIER_TYPE));

        return dto;
    }
}
