package au.org.raid.api.dto.legacy;

import au.org.raid.api.dto.LegacyRaid;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.idl.raidv2.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class RaidDtoFactory {
    private static final Map<String, String> DESCRIPTION_TYPE_MAP = Map.of(
            "Primary Description", SchemaValues.PRIMARY_DESCRIPTION_TYPE.getUri(),
            "Alternative Description", SchemaValues.ALTERNATIVE_DESCRIPTION_TYPE.getUri()
    );

    private static final Map<String, String> TITLE_TYPE_MAP = Map.of(
            "Primary Title", SchemaValues.PRIMARY_TITLE_TYPE.getUri(),
            "Alternative Title", SchemaValues.ALTERNATIVE_TITLE_TYPE.getUri()
    );

    private static final Map<String, String> ACCESS_TYPE_MAP = Map.of(
            "Open", SchemaValues.ACCESS_TYPE_OPEN.getUri(),
            "Closed", SchemaValues.ACCESS_TYPE_EMBARGOED.getUri()
    );

    public RaidDto create(final LegacyRaid legacyRaid) {
        final var alternateUrls = legacyRaid.getAlternateUrls() != null ?
                legacyRaid.getAlternateUrls().stream()
                        .map(legacyAlternateUrl -> new AlternateUrl().url(legacyAlternateUrl.getUrl()))
                        .toList() : null;

        final var embargoExpiry = LocalDate.now().plusMonths(18);

        final var access = new Access()
                .type(
                        new AccessType()
                                .id(ACCESS_TYPE_MAP.get(legacyRaid.getAccess().getType()))
                                .schemaUri(SchemaValues.ACCESS_TYPE_SCHEMA.getUri())
                )
                .embargoExpiry(legacyRaid.getAccess().getType().equals("Closed") ? embargoExpiry : null)
                .statement(new AccessStatement().text("Set to embargoed from closed during metadata uplift %s".formatted(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)))
                        .language(new Language().id("eng").schemaUri(SchemaValues.LANGUAGE_SCHEMA.getUri())));


        return new RaidDto()
                .identifier(new Id()
                                .id(legacyRaid.getId().getIdentifier())
                                .owner(new Owner()
                                        .schemaUri(SchemaValues.ROR_SCHEMA_URI.getUri())
                                        .id(legacyRaid.getId().getIdentifierOwner())
                                        .servicePoint(legacyRaid.getId().getIdentifierServicePoint())
                                )
                                .license("Creative Commons CC-0")
                                .schemaUri(SchemaValues.RAID_SCHEMA_URI.getUri())
                                .registrationAgency(new RegistrationAgency()
                                        .id(legacyRaid.getId().getRaidAgencyUrl())
                                        .schemaUri(SchemaValues.ROR_SCHEMA_URI.getUri()))
                                .version(legacyRaid.getId().getVersion())
                        // TODO: set raidAgencyUrl depending on environment

                )
                .title(legacyRaid.getTitles().stream().map(legacyTitle ->
                        new Title()
                                .text(legacyTitle.getTitle())
                                .type(new TitleType()
                                        .id(TITLE_TYPE_MAP.get(legacyTitle.getType()))
                                        .schemaUri(SchemaValues.TITLE_TYPE_SCHEMA.getUri()))
                                .startDate(legacyTitle.getStartDate())
                                .endDate(legacyTitle.getEndDate())

                ).toList())
                .description(legacyRaid.getDescriptions().stream()
                        .map(legacyDescription ->
                                new Description()
                                        .text(legacyDescription.getDescription())
                                        .type(new DescriptionType()
                                                .schemaUri(SchemaValues.DESCRIPTION_TYPE_SCHEMA.getUri())
                                                .id(DESCRIPTION_TYPE_MAP.get(legacyDescription.getType())))
                        ).toList()
                )
                .date(new Date()
                        .startDate(legacyRaid.getDates().getStartDate())
                        .endDate(legacyRaid.getDates().getEndDate())
                )
                .alternateUrl(alternateUrls)
                .access(access);
    }

}
