package au.org.raid.api.model.datacite.doi;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DatacitePublisher {
    private String name;
    private String publisherIdentifier;
    private String publisherIdentifierScheme;
    private String schemeUri;
}
