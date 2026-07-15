package au.org.raid.api.endpoint.raidv2;

import au.org.raid.api.exception.ValidationException;
import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;

/**
 * RAID-738: traditionalKnowledgeLabel withdrawn pending Indigenous co-design. Remove this advice when the field is reinstated.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class TraditionalKnowledgeLabelRejectionAdvice extends RequestBodyAdviceAdapter {
    private static final String FIELD_ID = "traditionalKnowledgeLabel";

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(final MethodParameter methodParameter, final Type targetType,
                             final Class<? extends HttpMessageConverter<?>> converterType) {
        return RaidCreateRequest.class.equals(targetType) || RaidUpdateRequest.class.equals(targetType);
    }

    @Override
    public HttpInputMessage beforeBodyRead(final HttpInputMessage inputMessage, final MethodParameter parameter,
                                            final Type targetType,
                                            final Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        final byte[] body = inputMessage.getBody().readAllBytes();

        try {
            final JsonNode root = objectMapper.readTree(body);
            if (root != null && root.isObject() && root.has(FIELD_ID)) {
                throw new ValidationException(List.of(new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("The traditionalKnowledgeLabel field is not currently supported.")));
            }
        } catch (final JsonProcessingException e) {
            // Malformed JSON - let normal deserialization handle/report it.
        }

        final HttpHeaders headers = inputMessage.getHeaders();
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(body);
            }

            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }
        };
    }
}
