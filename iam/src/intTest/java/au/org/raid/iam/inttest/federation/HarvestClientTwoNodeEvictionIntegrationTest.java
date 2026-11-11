package au.org.raid.iam.inttest.federation;

import au.org.raid.iam.inttest.JwksServer;
import au.org.raid.iam.inttest.KeycloakNode;
import au.org.raid.iam.inttest.SigningKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;
import java.util.regex.Pattern;

import static au.org.raid.iam.inttest.KeycloakNode.HARVEST_CLIENT_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Retiring a Registration Authority key on an agency that runs two clustered Keycloak nodes.
 *
 * <p>Keycloak caches the keys it fetched from the JWKS and only refetches on an unknown key ID, so a
 * key removed from the JWKS stays accepted until something forces a refetch. The Registration
 * Authority forces one by sending an assertion with an unknown key ID. This test establishes what
 * that does on a cluster: whether one eviction request reaches every node, or whether each node must
 * be sent its own.
 */
class HarvestClientTwoNodeEvictionIntegrationTest {

    /** Keycloak refuses to refetch a JWKS more often than this (PublicKeyStorageManager min interval). */
    private static final long REFETCH_INTERVAL_MILLIS = 11_000;

    private static Network network;
    private static PostgreSQLContainer<?> postgres;
    private static JwksServer jwks;
    private static KeycloakNode node1;
    private static KeycloakNode node2;

    @BeforeAll
    static void startCluster() {
        jwks = JwksServer.start();
        network = Network.newNetwork();
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withNetwork(network).withNetworkAliases("postgres")
                .withDatabaseName("keycloak").withUsername("keycloak").withPassword("keycloak");
        postgres.start();
        // One at a time, so only the first node imports the realm and creates the client.
        node1 = node("kc1");
        node2 = node("kc2");
    }

    private static KeycloakNode node(final String alias) {
        return KeycloakNode.builder()
                .production()
                .postgres(network, alias, "jdbc:postgresql://postgres:5432/keycloak", "keycloak", "keycloak")
                .jwksServer(jwks)
                .start();
    }

    @AfterAll
    static void stopAll() {
        if (node2 != null) node2.stop();
        if (node1 != null) node1.stop();
        if (postgres != null) postgres.stop();
        if (jwks != null) jwks.close();
        if (network != null) network.close();
    }

    private static int status(final KeycloakNode node, final SigningKey key) {
        return node.harvestToken(key.assertion(HARVEST_CLIENT_ID, node.issuer())).status();
    }

    /** An assertion naming a key ID no JWKS holds: rejected, but it makes the node refetch. */
    private static int evictionRequest(final KeycloakNode node, final SigningKey signer) {
        return node.harvestToken(signer.assertion(HARVEST_CLIENT_ID, node.issuer(),
                "evict-" + UUID.randomUUID(), 60, UUID.randomUUID().toString())).status();
    }

    @Test
    void retiringAKeyNeedsAnEvictionRequestOnEveryNode() throws InterruptedException {
        // The nodes formed one cluster, as a multi-node agency deployment does.
        final var twoMemberView = Pattern.compile("ISPN000094: Received new cluster view.*\\(2\\)");
        assertThat("node2 joined node1's cluster", twoMemberView.matcher(node2.logs()).find(), is(true));
        assertThat(node1.harvestClients("raid").size(), is(1));

        final var retiring = SigningKey.generate("ra-2026");
        final var replacement = SigningKey.generate("ra-2027");
        jwks.publish(retiring);

        // Both nodes fetch and cache the key that is about to be retired.
        assertThat(status(node1, retiring), is(200));
        assertThat(status(node2, retiring), is(200));

        // Rotation: the JWKS now lists only the replacement.
        jwks.publish(replacement);
        Thread.sleep(REFETCH_INTERVAL_MILLIS);
        final var fetchesBefore = jwks.fetches();

        // Without any eviction both nodes still accept the retired key from cache.
        assertThat(status(node1, retiring), is(200));
        assertThat(status(node2, retiring), is(200));

        // One eviction request, sent to node1 only.
        assertThat(evictionRequest(node1, replacement), is(400));
        assertThat("node1 refetched the JWKS", jwks.fetches(), greaterThan(fetchesBefore));
        assertThat("node1 no longer accepts the retired key", status(node1, retiring), is(400));

        // Each node keeps its own key cache: node2 is unaffected until it is sent its own request.
        assertThat("node2 still accepts the retired key", status(node2, retiring), is(200));
        assertThat(evictionRequest(node2, replacement), is(400));
        assertThat("node2 no longer accepts the retired key", status(node2, retiring), is(400));

        // The replacement works everywhere.
        assertThat(status(node1, replacement), is(200));
        assertThat(status(node2, replacement), is(200));
    }
}
