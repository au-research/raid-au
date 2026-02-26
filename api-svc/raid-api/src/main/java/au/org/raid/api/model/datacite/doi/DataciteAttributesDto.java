package au.org.raid.api.model.datacite.doi;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class DataciteAttributesDto {
    private String prefix;
    private String doi;
    private DatacitePublisher publisher;
    private String publicationYear;
    private List<DataciteTitle> titles;
    private List<DataciteCreator> creators;
    private List<DataciteDate> dates;
    private List<DataciteContributor> contributors;
    private List<DataciteDescription> descriptions;
    private List<DataciteRelatedIdentifier> relatedIdentifiers;
    private List<DataciteRight> rightsList;
    private DataciteTypes types;
    private List<DataciteAlternateIdentifier> alternateIdentifiers;
    private List<DataciteFundingReference> fundingReferences;
    private String event;
    private String url;
}
