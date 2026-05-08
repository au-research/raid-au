package au.org.raid.inttest.service;

import tools.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RaidApiExceptionDecoder extends ErrorDecoder.Default {
    private static final Logger log = LoggerFactory.getLogger(RaidApiExceptionDecoder.class);
    private ObjectMapper mapper;

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
        } catch (IOException e) {
            log.error("could not map error body", e);
            return feignDefaultEx;
        }

    }
}