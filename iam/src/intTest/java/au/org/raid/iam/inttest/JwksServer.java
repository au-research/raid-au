package au.org.raid.iam.inttest;

import com.sun.net.httpserver.HttpServer;
import org.testcontainers.Testcontainers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.keycloak.util.JsonSerialization.writeValueAsString;

/**
 * Serves a JWKS from the test JVM, standing in for the Registration Authority's published key set.
 * The key set can be swapped mid-test to rehearse rotation. Containers reach it through
 * Testcontainers' host port bridge at {@link #urlForContainers()}, over plain http, which is why the
 * suite sets the harvest client's plain-http opt-in.
 */
public final class JwksServer implements AutoCloseable {

    private final HttpServer server;
    private final AtomicReference<byte[]> body = new AtomicReference<>("{\"keys\":[]}".getBytes(StandardCharsets.UTF_8));
    private final AtomicInteger fetches = new AtomicInteger();

    private JwksServer(final HttpServer server) {
        this.server = server;
    }

    public static JwksServer start() {
        try {
            final var server = HttpServer.create(new InetSocketAddress(0), 0);
            final var jwks = new JwksServer(server);
            server.createContext("/jwks.json", exchange -> {
                jwks.fetches.incrementAndGet();
                final var bytes = jwks.body.get();
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (var out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            });
            server.start();
            Testcontainers.exposeHostPorts(server.getAddress().getPort());
            return jwks;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void publish(final SigningKey... keys) {
        try {
            final var json = writeValueAsString(Map.of("keys", Arrays.stream(keys).map(SigningKey::jwk).toList()));
            body.set(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String urlForContainers() {
        return "http://host.testcontainers.internal:" + server.getAddress().getPort() + "/jwks.json";
    }

    public int fetches() {
        return fetches.get();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
