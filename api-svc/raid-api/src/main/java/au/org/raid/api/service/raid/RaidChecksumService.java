package au.org.raid.api.service.raid;

import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

@Component
@RequiredArgsConstructor
public class RaidChecksumService {
    private final ObjectMapper objectMapper;

//    @SneakyThrows
//    public String create(final RaidRecord raidRecord) {
//        final var metadata = raidRecord.getMetadata().data();
//        // A bit nasty to turn a string into an object and then back into a string but the json needs to be in the
//        // same order for the checksums to be identical. This relies on MapperFeature.SORT_PROPERTIES_ALPHABETICALLY
//        // property set on the ObjectMapper bean
//
//        final var raid = objectMapper.readValue(metadata, RaidDto.class);
//        final var json = objectMapper.writeValueAsString(raid);
//        return DigestUtils.md5DigestAsHex(json.getBytes());
//    }

    public String fromRaidUpdateRequest(final RaidUpdateRequest raid) {
        final RaidUpdateRequest r = SerializationUtils.clone(raid);
        r.metadata(null);
        return create(r);
    }

    public String fromRaidDto(final RaidDto raid) {
        final RaidDto r = SerializationUtils.clone(raid);
        r.metadata(null);
        return create(r);
    }


    @SneakyThrows
    public <T>String create(final T object) {
        return DigestUtils.md5DigestAsHex(objectMapper.writeValueAsString(object).getBytes());
    }
}