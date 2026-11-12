package au.org.raid.iam.inttest;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.keycloak.util.JsonSerialization.writeValueAsString;

/**
 * An RSA key generated for one test run, standing in for the Registration Authority's KMS key.
 * It signs RFC 7523 client assertions exactly as the Federation API does (RS256, iss = sub =
 * client ID, aud = realm issuer, single-use jti, short expiry).
 */
public final class SigningKey {

    private final String kid;
    private final KeyPair keyPair;

    private SigningKey(final String kid, final KeyPair keyPair) {
        this.kid = kid;
        this.keyPair = keyPair;
    }

    public static SigningKey generate(final String kid) {
        try {
            final var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return new SigningKey(kid, generator.generateKeyPair());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String kid() {
        return kid;
    }

    /** The public half as a JWK, as it would appear in the Registration Authority's JWKS. */
    public Map<String, Object> jwk() {
        final var publicKey = (RSAPublicKey) keyPair.getPublic();
        final var jwk = new LinkedHashMap<String, Object>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", kid);
        jwk.put("n", base64Url(unsigned(publicKey.getModulus())));
        jwk.put("e", base64Url(unsigned(publicKey.getPublicExponent())));
        return jwk;
    }

    public String assertion(final String clientId, final String audience) {
        return assertion(clientId, audience, kid, 60, UUID.randomUUID().toString());
    }

    /** Signs with this key but labels the header with {@code headerKid}, which may be unknown. */
    public String assertion(final String clientId, final String audience, final String headerKid,
                            final long expiresInSeconds, final String jti) {
        final var now = Instant.now().getEpochSecond();
        final var header = new LinkedHashMap<String, Object>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");
        header.put("kid", headerKid);
        final var claims = new LinkedHashMap<String, Object>();
        claims.put("iss", clientId);
        claims.put("sub", clientId);
        claims.put("aud", audience);
        claims.put("jti", jti);
        claims.put("iat", now);
        claims.put("exp", now + expiresInSeconds);
        try {
            final var signingInput = base64Url(writeValueAsString(header).getBytes(StandardCharsets.UTF_8)) + "." +
                    base64Url(writeValueAsString(claims).getBytes(StandardCharsets.UTF_8));
            final var signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(keyPair.getPrivate());
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + base64Url(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String base64Url(final byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] unsigned(final BigInteger value) {
        final var bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            final var trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return bytes;
    }
}
