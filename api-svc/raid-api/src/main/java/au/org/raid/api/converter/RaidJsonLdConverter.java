package au.org.raid.api.converter;

import au.org.raid.api.factory.schemaorg.ResearchProjectFactory;
import au.org.raid.api.service.rdf.RaidRdfService;
import au.org.raid.idl.raidv2.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Converter for RaidDto to JSON-LD format using Schema.org vocabulary
 * This implementation provides direct mapping to Schema.org types and properties
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RaidJsonLdConverter implements HttpMessageConverter<RaidDto> {
    private static final MediaType JSON_LD_MEDIA_TYPE = MediaType.valueOf("application/ld+json");
    
    private final ObjectMapper objectMapper;
    private final ResearchProjectFactory researchProjectFactory;

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false; // We only support writing, not reading
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return RaidDto.class.isAssignableFrom(clazz) && 
               JSON_LD_MEDIA_TYPE.isCompatibleWith(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return List.of(JSON_LD_MEDIA_TYPE);
    }

    @Override
    public RaidDto read(Class<? extends RaidDto> clazz, HttpInputMessage inputMessage) 
            throws HttpMessageNotReadableException {
        throw new UnsupportedOperationException("Reading JSON-LD format is not supported");
    }

    @Override
    @SneakyThrows
    public void write(RaidDto raidDto, MediaType contentType, HttpOutputMessage outputMessage) 
            throws IOException, HttpMessageNotWritableException {
        // Set the JSON-LD content type in the response
        outputMessage.getHeaders().setContentType(JSON_LD_MEDIA_TYPE);

        final var researchProject = this.researchProjectFactory.create(raidDto);
        

        // Write the result to the response body
        outputMessage.getBody().write(objectMapper.writeValueAsBytes(researchProject));
    }
}