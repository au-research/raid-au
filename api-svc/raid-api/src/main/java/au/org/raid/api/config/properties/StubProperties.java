package au.org.raid.api.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "raid.stub")
public class StubProperties {
    private Doi doi;
    private Ror ror;
    private Orcid orcid;
    private GeoNames geoNames;
    private Apids apids;
    private OpenStreetMap openStreetMap;
    private Isni isni;

    @Data
    public static class Doi {
        private boolean enabled;
        private Long delay;
    }
    @Data
    public static class OpenStreetMap {
        private boolean enabled;
        private Long delay;
    }
    @Data
    public static class Apids {
        private boolean enabled;
        private Long delay;
    }

    @Data
    public static class GeoNames {
        private boolean enabled;
        private Long delay;
    }

    @Data
    public static class Orcid {
        private boolean enabled;
        private Long delay;
    }

    @Data
    public static class Ror {
        private boolean enabled;
        private Long delay;
    }

    @Data
    public static class Isni {
        private boolean enabled;
        private Long delay;
    }
}
