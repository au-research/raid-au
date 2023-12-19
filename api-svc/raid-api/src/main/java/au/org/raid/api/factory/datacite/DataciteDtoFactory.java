package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.*;
import au.org.raid.api.service.datacite.DataciteService;
import au.org.raid.api.spring.config.DataciteProperties;
import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataciteDtoFactory {

    private final DataciteProperties properties = new DataciteProperties();
    private final DataciteService dataciteService = new DataciteService(properties);

    @SneakyThrows
    public DataciteDto create(RaidCreateRequest request, String handle) {
        if (request == null) {
            return null;
        }

        DataciteTitleFactory dataciteTitleFactory = new DataciteTitleFactory();
        DataciteCreatorFactory dataciteCreatorFactory = new DataciteCreatorFactory();
        DataciteDateFactory dataciteDateFactory = new DataciteDateFactory();
        DataciteContributorFactory dataciteContributorFactory = new DataciteContributorFactory();
        DataciteDescriptionFactory dataciteDescriptionFactory = new DataciteDescriptionFactory();
        DataciteRelatedIdentifierFactory dataciteRelatedIdentifierFactory = new DataciteRelatedIdentifierFactory();
        DataciteRightFactory dataciteRightFactory = new DataciteRightFactory();
        DataciteTypesFactory dataciteTypesFactory = new DataciteTypesFactory();

        DataciteAttributesDtoFactory dataciteAttributesDtoFactory = new DataciteAttributesDtoFactory(dataciteTitleFactory, dataciteCreatorFactory, dataciteDateFactory, dataciteContributorFactory, dataciteDescriptionFactory, dataciteRelatedIdentifierFactory, dataciteRightFactory, dataciteTypesFactory);

        DataciteAttributesDto dataciteAttributesDto = dataciteAttributesDtoFactory.create(request, handle);

        DataciteIdentifier dataciteIdentifier = new DataciteIdentifier();
        dataciteIdentifier.setIdentifierType("DOI");
        dataciteIdentifier.setIdentifier(handle);

        return new DataciteDto().setSchemaVersion("http://datacite.org/schema/kernel-4").setType("dois").setAttributes(dataciteAttributesDto).setIdentifier(dataciteIdentifier);
    }

    @SneakyThrows
    public DataciteDto create(RaidUpdateRequest request, String handle) {
        if (request == null) {
            return null;
        }

        DataciteTitleFactory dataciteTitleFactory = new DataciteTitleFactory();
        DataciteCreatorFactory dataciteCreatorFactory = new DataciteCreatorFactory();
        DataciteDateFactory dataciteDateFactory = new DataciteDateFactory();
        DataciteContributorFactory dataciteContributorFactory = new DataciteContributorFactory();
        DataciteDescriptionFactory dataciteDescriptionFactory = new DataciteDescriptionFactory();
        DataciteRelatedIdentifierFactory dataciteRelatedIdentifierFactory = new DataciteRelatedIdentifierFactory();
        DataciteRightFactory dataciteRightFactory = new DataciteRightFactory();
        DataciteTypesFactory dataciteTypesFactory = new DataciteTypesFactory();

        DataciteAttributesDtoFactory dataciteAttributesDtoFactory = new DataciteAttributesDtoFactory(dataciteTitleFactory, dataciteCreatorFactory, dataciteDateFactory, dataciteContributorFactory, dataciteDescriptionFactory, dataciteRelatedIdentifierFactory, dataciteRightFactory, dataciteTypesFactory);

        DataciteAttributesDto dataciteAttributesDto = dataciteAttributesDtoFactory.create(request, handle);

        DataciteIdentifier dataciteIdentifier = new DataciteIdentifier();
        dataciteIdentifier.setIdentifierType("DOI");

        dataciteIdentifier.setIdentifier(handle);


        return new DataciteDto()
                .setSchemaVersion("http://datacite.org/schema/kernel-4")
                .setType("dois")
                .setAttributes(dataciteAttributesDto)
                .setIdentifier(dataciteIdentifier);
    }

}
