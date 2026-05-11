package au.org.raid.inttest.service;

import feign.FeignException;
import feign.Response;
import feign.codec.DefaultErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class RaidApiExceptionDecoder extends DefaultErrorDecoder {
    private static final Logger log = LoggerFactory.getLogger(RaidApiExceptionDecoder.class);
    private final ObjectMapper mapper;

    public RaidApiExceptionDecoder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        Exception feignDefaultEx = super.decode(methodKey, response);

        if (!(feignDefaultEx instanceof FeignException.BadRequest badRequest)) {
            return feignDefaultEx;
        }

        if (badRequest.responseBody().isEmpty()) {
            return feignDefaultEx;
        }

        try {
            byte[] body = badRequest.responseBody().get().array();
            var validEx = mapper.readValue(
                    body, RaidApiValidationException.class);
            validEx.setBadRequest(badRequest);

            return validEx;
        } catch (JacksonException e) {
            log.error("could not map error body", e);
            return feignDefaultEx;
        }

    }
}