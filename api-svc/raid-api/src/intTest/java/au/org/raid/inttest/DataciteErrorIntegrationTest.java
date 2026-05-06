package au.org.raid.inttest;

import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class DataciteErrorIntegrationTest extends AbstractIntegrationTest {

    private static final String MOCKSERVER_EXPECTATION_URL = "http://localhost:1080/mockserver/expectation";
    private static final String EXPECTATION_ID = "datacite-error-integration-test";

    @Autowired
    private RestTemplate restTemplate;

    @AfterEach
    void cleanUpMockServerExpectation() {
        final var cleanup = """
                [{
                    "id": "%s",
                    "priority": 0,
                    "httpRequest": {
                        "method": "POST",
                        "path": "/dois"
                    },
                    "httpResponse": {
                        "statusCode": 201
                    },
                    "times": {
                        "remainingTimes": 0,
                        "unlimited": false
                    }
                }]
                """.formatted(EXPECTATION_ID);

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.put(MOCKSERVER_EXPECTATION_URL, new HttpEntity<>(cleanup, headers));
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Mint fails when DataCite returns an error")
    void mintFailsOnDataciteError() {
        createDataciteErrorExpectation();

        try {
            raidApi.mintRaid(createRequest);
            fail("Expected mint to fail when DataCite returns 429");
        } catch (FeignException e) {
            assertThat(e.status()).isEqualTo(500);
        }
    }

    private void createDataciteErrorExpectation() {
        final var expectation = """
                [{
                    "id": "%s",
                    "priority": 10,
                    "httpRequest": {
                        "method": "POST",
                        "path": "/dois",
                        "headers": {
                            "Authorization": ["Basic ZGF0YWNpdGUtdXNlcm5hbWU6ZGF0YWNpdGUtcGFzc3dvcmQ="],
                            "Content-Type": ["application/json"]
                        }
                    },
                    "httpResponse": {
                        "statusCode": 429,
                        "headers": {
                            "Content-Type": ["application/json"]
                        },
                        "body": {
                            "errors": [{"status": "429", "title": "Too Many Requests"}]
                        }
                    },
                    "times": {
                        "remainingTimes": 1,
                        "unlimited": false
                    }
                }]
                """.formatted(EXPECTATION_ID);

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.put(MOCKSERVER_EXPECTATION_URL, new HttpEntity<>(expectation, headers));
    }
}
