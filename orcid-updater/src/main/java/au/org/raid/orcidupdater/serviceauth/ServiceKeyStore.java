package au.org.raid.orcidupdater.serviceauth;

import au.org.raid.orcidupdater.crypto.ServiceKeyCipher;
import au.org.raid.orcidupdater.serviceauth.ServiceKeyRepository.StoredKey;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;

/**
 * The updater's RSA key for signing token requests to Keycloak (private_key_jwt, RAID-1005). It
 * is created on first use and kept, encrypted, in the database, so every instance signs with the
 * same key and Keycloak only ever sees one. If the stored key can't be decrypted (the token
 * encryption key was changed), a new one replaces it; Keycloak picks it up from the JWKS endpoint.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceKeyStore {
    private static final int KEY_BITS = 2048;

    private final ServiceKeyRepository repository;
    private final ServiceKeyCipher cipher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    private volatile RSAKey key;

    /** The key pair, private half included. Never log or publish this; use {@link #publicJwk()}. */
    public RSAKey signingKey() {
        var current = key;
        if (current == null) {
            synchronized (this) {
                current = key;
                if (current == null) {
                    current = transactionTemplate.execute(status -> loadOrCreate());
                    key = current;
                }
            }
        }
        return current;
    }

    public RSAKey publicJwk() {
        return signingKey().toPublicJWK();
    }

    private RSAKey loadOrCreate() {
        repository.lock();
        final var stored = repository.findCurrent();
        if (stored.isPresent()) {
            final var privateKey = cipher.decrypt(stored.get().privateKey(), stored.get().keyId());
            if (privateKey.isPresent()) {
                return rsaKey(stored.get().keyId(), stored.get().publicKey(), privateKey.get());
            }
            log.warn("The stored service key {} can't be decrypted (was the token encryption key changed?); "
                    + "creating a new one", stored.get().keyId());
        }
        return create();
    }

    private RSAKey create() {
        try {
            final var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_BITS);
            final var pair = generator.generateKeyPair();
            final var keyId = new RSAKey.Builder((RSAPublicKey) pair.getPublic()).build().computeThumbprint().toString();
            repository.replace(new StoredKey(keyId, cipher.encrypt(pair.getPrivate().getEncoded(), keyId),
                    pair.getPublic().getEncoded()), clock.instant());
            log.info("Created service key {} for signing token requests", keyId);
            return rsaKey(keyId, pair.getPublic().getEncoded(), pair.getPrivate().getEncoded());
        } catch (final GeneralSecurityException | JOSEException e) {
            throw new IllegalStateException("Couldn't create the service key", e);
        }
    }

    private static RSAKey rsaKey(final String keyId, final byte[] publicKey, final byte[] privateKey) {
        try {
            final var factory = KeyFactory.getInstance("RSA");
            return new RSAKey.Builder((RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(publicKey)))
                    .privateKey((RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(privateKey)))
                    .keyID(keyId)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .build();
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("The stored service key is unreadable", e);
        }
    }
}
