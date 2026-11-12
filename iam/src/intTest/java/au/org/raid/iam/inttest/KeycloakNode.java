package au.org.raid.iam.inttest;

import com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.keycloak.util.JsonSerialization.mapper;

/**
 * One Keycloak running the built raid-iam.jar with the shipped raid realm imported, plus the HTTP
 * helpers the federation harvest tests need: token requests and the admin REST API.
 *
 * <p>The image and paths come from system properties set by the {@code intTest} Gradle task, so the
 * suite always runs the Keycloak version in {@code iam/Dockerfile} against the jar just built.
 */
public final class KeycloakNode implements AutoCloseable {

    public static final String REALM = "raid";
    public static final String HARVEST_CLIENT_ID = "raid-federation-harvester";
    public static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final GenericContainer<?> container;

    private KeycloakNode(final GenericContainer<?> container) {
        this.container = container;
    }

    /** Builder for a node. Defaults: dev mode, the built jar, the embedded H2 database. */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Path jar = Path.of(requiredProperty("raid.iam.jar"));
        private final Map<String, String> env = new LinkedHashMap<>();
        private Network network;
        private String alias;
        private boolean production;

        public Builder jar(final Path jar) {
            this.jar = jar;
            return this;
        }

        public Builder env(final String name, final String value) {
            env.put(name, value);
            return this;
        }

        /** Trust keys served by a test {@link JwksServer} (plain http, so the opt-in is set too). */
        public Builder jwksServer(final JwksServer jwks) {
            env.put("RAID_FEDERATION_HARVEST_JWKS_URL", jwks.urlForContainers());
            env.put("RAID_FEDERATION_HARVEST_ALLOW_HTTP_JWKS_URL", "true");
            return this;
        }

        /** Use a shared Postgres, so a later node sees the same realm (restart, cluster). */
        public Builder postgres(final Network network, final String alias, final String jdbcUrl,
                                final String username, final String password) {
            this.network = network;
            this.alias = alias;
            env.put("KC_DB", "postgres");
            env.put("KC_DB_URL", jdbcUrl);
            env.put("KC_DB_USERNAME", username);
            env.put("KC_DB_PASSWORD", password);
            return this;
        }

        /**
         * Production mode ({@code kc.sh start}) with the clustered Infinispan cache, as a multi-node
         * agency deployment runs. Nodes sharing a database discover each other through JDBC_PING.
         */
        public Builder production() {
            this.production = true;
            return this;
        }

        @SuppressWarnings("resource")
        public KeycloakNode start() {
            final var container = new GenericContainer<>(requiredProperty("raid.iam.keycloakImage"))
                    .withExposedPorts(8080)
                    .withCopyFileToContainer(MountableFile.forHostPath(jar), "/opt/keycloak/providers/raid-iam.jar")
                    .withCopyFileToContainer(MountableFile.forHostPath(requiredProperty("raid.iam.realmDir")),
                            "/opt/keycloak/data/import")
                    .withAccessToHost(true)
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                    .withEnv("KC_HOSTNAME_STRICT", "false")
                    .withEnv("KC_HTTP_ENABLED", "true")
                    .withEnv(env)
                    .waitingFor(Wait.forHttp("/realms/" + REALM).forPort(8080)
                            .withStartupTimeout(Duration.ofMinutes(6)));
            if (production) {
                container.withEnv("KC_CACHE", "ispn").withCommand("start", "--import-realm");
            } else {
                container.withCommand("start-dev", "--import-realm");
            }
            if (network != null) {
                container.withNetwork(network);
                if (alias != null) {
                    container.withNetworkAliases(alias);
                }
            }
            container.start();
            return new KeycloakNode(container);
        }
    }

    public String baseUrl() {
        return "http://" + container.getHost() + ":" + container.getMappedPort(8080);
    }

    /** The realm issuer as this node reports it; the audience a client assertion must carry. */
    public String issuer() {
        return get(baseUrl() + "/realms/" + REALM + "/.well-known/openid-configuration", null).body().get("issuer").asText();
    }

    public String logs() {
        return container.getLogs();
    }

    public void stop() {
        container.stop();
    }

    @Override
    public void close() {
        stop();
    }

    // --- token endpoint -------------------------------------------------------------------

    public Response clientCredentials(final Map<String, String> form) {
        final var all = new LinkedHashMap<String, String>();
        all.put("grant_type", "client_credentials");
        all.putAll(form);
        return send(HttpRequest.newBuilder(URI.create(baseUrl() + "/realms/" + REALM + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(all))).build());
    }

    public Response harvestToken(final String assertion) {
        return clientCredentials(Map.of(
                "client_id", HARVEST_CLIENT_ID,
                "client_assertion_type", CLIENT_ASSERTION_TYPE,
                "client_assertion", assertion));
    }

    /** Decodes the claims of an access token without verifying it (the test trusts its own Keycloak). */
    public static JsonNode claims(final String accessToken) {
        try {
            return mapper.readTree(Base64.getUrlDecoder().decode(accessToken.split("\\.")[1]));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // --- admin REST API -------------------------------------------------------------------

    public String adminToken() {
        final var response = send(HttpRequest.newBuilder(URI.create(baseUrl() + "/realms/master/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(Map.of(
                        "grant_type", "password", "client_id", "admin-cli",
                        "username", "admin", "password", "admin")))).build());
        return response.body().get("access_token").asText();
    }

    public Response adminGet(final String path) {
        return get(baseUrl() + "/admin/realms" + path, adminToken());
    }

    public Response adminPut(final String path, final Object body) {
        return adminWrite("PUT", path, body);
    }

    public Response adminPost(final String path, final Object body) {
        return adminWrite("POST", path, body);
    }

    private Response adminWrite(final String method, final String path, final Object body) {
        try {
            return send(HttpRequest.newBuilder(URI.create(baseUrl() + "/admin/realms" + path))
                    .header("Authorization", "Bearer " + adminToken())
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Every client in {@code realm} with the harvest client ID (normally zero or one). */
    public JsonNode harvestClients(final String realm) {
        return adminGet("/" + realm + "/clients?clientId=" + HARVEST_CLIENT_ID).body();
    }

    public JsonNode harvestClient() {
        final var clients = harvestClients(REALM);
        if (clients.size() != 1) {
            throw new IllegalStateException("Expected one harvest client, found " + clients.size());
        }
        return clients.get(0);
    }

    // --- plumbing -------------------------------------------------------------------------

    public record Response(int status, JsonNode body) {
        public String error() {
            return body != null && body.has("error") ? body.get("error").asText() : null;
        }
    }

    private static Response get(final String url, final String bearer) {
        final var builder = HttpRequest.newBuilder(URI.create(url)).GET();
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        return send(builder.build());
    }

    private static Response send(final HttpRequest request) {
        try {
            final var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            final var text = response.body();
            JsonNode body = null;
            if (text != null && !text.isBlank()) {
                try {
                    body = mapper.readTree(text);
                } catch (IOException e) {
                    body = mapper.getNodeFactory().textNode(text);
                }
            }
            return new Response(response.statusCode(), body);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static String formEncode(final Map<String, String> form) {
        return form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    static String requiredProperty(final String name) {
        final var value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("System property " + name + " is not set; run through ./gradlew :iam:intTest");
        }
        return value;
    }
}
