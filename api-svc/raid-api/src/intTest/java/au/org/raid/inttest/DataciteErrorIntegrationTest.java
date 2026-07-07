package au.org.raid.inttest;

import feign.FeignException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class DataciteErrorIntegrationTest extends AbstractIntegrationTest {

    private static final String MOCKSERVER_HOST = "localhost";
    private static final int MOCKSERVER_PORT = 1080;
    private static final int MOCKSERVER_CONNECT_TIMEOUT_MILLIS = 2000;

    // Sentinel matched by the static 429 expectation in docker-compose/mockserver/expectations.json
    private static final String DATACITE_ERROR_TITLE_SENTINEL = "RAID-731-DATACITE-ERROR";

    @BeforeEach
    void checkMockServerIsReachable() {
        Assumptions.assumeTrue(isMockServerReachable(), "mockserver not reachable - skipping DataCite error test");
    }

    private static boolean isMockServerReachable() {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(MOCKSERVER_HOST, MOCKSERVER_PORT), MOCKSERVER_CONNECT_TIMEOUT_MILLIS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Test
    @DisplayName("Mint fails when DataCite returns an error")
    void mintFailsOnDataciteError() {
        createRequest.getTitle().get(0).setText(DATACITE_ERROR_TITLE_SENTINEL + " " + UUID.randomUUID());

        try {
            raidApi.mintRaid(createRequest);
            fail("Expected mint to fail when DataCite returns 429");
        } catch (FeignException e) {
            assertThat(e.status()).isEqualTo(500);
        }
    }
}
