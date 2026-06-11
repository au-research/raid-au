package au.org.raid.api.converter;

import au.org.raid.api.service.rdf.RaidRdfService;
import au.org.raid.idl.raidv2.model.RaidDto;
import lombok.RequiredArgsConstructor;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Converter for RaidDto to RDF/XML format
 */
@Component
@RequiredArgsConstructor
public class RaidRdfXmlConverter implements HttpMessageConverter<RaidDto> {
    private static final MediaType RDFXML_MEDIA_TYPE = new MediaType("application", "rdf+xml");
    
    private final RaidRdfService raidRdfService;

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false; // We only support writing, not reading
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return RaidDto.class.isAssignableFrom(clazz) && RDFXML_MEDIA_TYPE.isCompatibleWith(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return List.of(RDFXML_MEDIA_TYPE);
    }

    @Override
    public RaidDto read(Class<? extends RaidDto> clazz, HttpInputMessage inputMessage) 
            throws HttpMessageNotReadableException {
        throw new UnsupportedOperationException("Reading RDF/XML format is not supported");
    }

    @Override
    public void write(RaidDto raidDto, MediaType contentType, HttpOutputMessage outputMessage) 
            throws IOException, HttpMessageNotWritableException {
        var model = raidRdfService.toRdfModel(raidDto);
        outputMessage.getHeaders().setContentType(RDFXML_MEDIA_TYPE);
        RDFDataMgr.write(outputMessage.getBody(), model, RDFFormat.RDFXML);
    }
}