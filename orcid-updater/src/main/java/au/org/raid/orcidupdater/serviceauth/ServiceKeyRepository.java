package au.org.raid.orcidupdater.serviceauth;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ServiceKeyRepository {
    private static final long LOCK_KEY = 1_005L;

    private final JdbcClient jdbcClient;

    public record StoredKey(String keyId, byte[] privateKey, byte[] publicKey) {
    }

    /** Serialises key creation across updater instances until the transaction ends. */
    public void lock() {
        jdbcClient.sql("SELECT pg_advisory_xact_lock(:key)").param("key", LOCK_KEY).query().singleValue();
    }

    public Optional<StoredKey> findCurrent() {
        return jdbcClient.sql("SELECT key_id, private_key, public_key FROM service_key ORDER BY created_at DESC LIMIT 1")
                .query((rs, row) -> new StoredKey(rs.getString("key_id"), rs.getBytes("private_key"),
                        rs.getBytes("public_key")))
                .optional();
    }

    /** Stores a new key and drops the others: only the newest is ever used or published. */
    public void replace(final StoredKey key, final Instant createdAt) {
        jdbcClient.sql("DELETE FROM service_key").update();
        jdbcClient.sql("""
                        INSERT INTO service_key (key_id, private_key, public_key, created_at)
                        VALUES (:keyId, :privateKey, :publicKey, :createdAt)
                        """)
                .param("keyId", key.keyId())
                .param("privateKey", key.privateKey())
                .param("publicKey", key.publicKey())
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .update();
    }
}
