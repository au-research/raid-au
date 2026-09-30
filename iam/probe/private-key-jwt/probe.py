#!/usr/bin/env python3
"""RAID-836 probe: does Keycloak 26.6.2 accept private_key_jwt (RFC 7523) client
authentication for a raid-dumper service-account client, with keys taken from a
remote JWKS URL (and, alternatively, an embedded certificate)?

Standard library + openssl only. Driven by run-probe.sh, which starts Keycloak
and the JWKS server. Admin REST is used here only to stand the fixture up; the
real implementation would set the same client fields in-process from the
PostMigrationEvent boot hook.
"""
import base64
import hashlib
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

KC = os.environ.get("KC_URL", "http://localhost:8095")
REALM = "probe"
CLIENT_ID = "raid-federation-harvester"
EMBEDDED_CLIENT_ID = "raid-federation-harvester-embedded"
MULTI_KEY_CLIENT_ID = "raid-federation-harvester-multi-key"
LIFESPAN_CLIENT_ID = "raid-federation-harvester-lifespan"
WORK = os.environ["WORK_DIR"]
JWKS_URL_IN_CONTAINER = os.environ["JWKS_URL_IN_CONTAINER"]
ISSUER = f"{KC}/realms/{REALM}"
TOKEN_URL = f"{ISSUER}/protocol/openid-connect/token"

results = []


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def b64url_decode(s: str) -> bytes:
    return base64.urlsafe_b64decode(s + "=" * (-len(s) % 4))


def key_path(name: str) -> str:
    return os.path.join(WORK, f"key-{name}.pem")


def jwk_for(name: str) -> dict:
    out = subprocess.run(["openssl", "rsa", "-in", key_path(name), "-noout", "-modulus"],
                         check=True, capture_output=True, text=True).stdout
    modulus = bytes.fromhex(out.strip().split("=", 1)[1])
    return {"kty": "RSA", "use": "sig", "alg": "RS256", "kid": name,
            "n": b64url(modulus), "e": "AQAB"}


def spki_thumbprint(name: str) -> str:
    """Keycloak's KeyUtils.createKeyId: base64url(SHA-256(SubjectPublicKeyInfo DER))."""
    der = subprocess.run(["openssl", "rsa", "-in", key_path(name), "-pubout", "-outform", "DER"],
                         check=True, capture_output=True).stdout
    return b64url(hashlib.sha256(der).digest())


def write_jwks(names):
    with open(os.path.join(WORK, "jwks", "jwks.json"), "w") as f:
        json.dump({"keys": [jwk_for(n) for n in names]}, f)


def sign_assertion(key: str, kid: str, client_id: str = CLIENT_ID, aud: str = ISSUER,
                   exp_offset: int = 60, jti: str = None) -> str:
    now = int(time.time())
    header = {"alg": "RS256", "typ": "JWT"}
    if kid:
        header["kid"] = kid
    claims = {"iss": client_id, "sub": client_id, "aud": aud,
              "jti": jti or str(uuid.uuid4()), "iat": now, "exp": now + exp_offset}
    signing_input = f"{b64url(json.dumps(header).encode())}.{b64url(json.dumps(claims).encode())}"
    sig = subprocess.run(["openssl", "dgst", "-sha256", "-sign", key_path(key)],
                         input=signing_input.encode(), check=True, capture_output=True).stdout
    return f"{signing_input}.{b64url(sig)}"


def http(method, url, data=None, token=None, form=False):
    headers = {}
    body = None
    if data is not None:
        if form:
            body = urllib.parse.urlencode(data).encode()
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        else:
            body = json.dumps(data).encode()
            headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read()
            return resp.status, (json.loads(raw) if raw else None), resp.headers
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw), e.headers
        except ValueError:
            return e.code, raw.decode(errors="replace"), e.headers


def token_request(assertion=None, client_id=CLIENT_ID, secret=None):
    form = {"grant_type": "client_credentials", "client_id": client_id}
    if assertion:
        form["client_assertion_type"] = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
        form["client_assertion"] = assertion
    if secret:
        form["client_secret"] = secret
    return http("POST", TOKEN_URL, form, form=True)


def record(name, expect_ok, status, body, extra=""):
    ok = (status == 200) == expect_ok
    detail = (f"{body.get('error')}: {body.get('error_description')}"
              if isinstance(body, dict) and status != 200 else "")
    results.append((name, "PASS" if ok else "FAIL", status, detail or extra))
    print(f"[{'PASS' if ok else 'FAIL'}] {name}: HTTP {status} {detail or extra}", flush=True)
    return ok


def roles_in(access_token):
    claims = json.loads(b64url_decode(access_token.split(".")[1]))
    return sorted(claims.get("realm_access", {}).get("roles", [])), claims


# ---------------------------------------------------------------- fixture setup
def admin_token():
    status, body, _ = http("POST", f"{KC}/realms/master/protocol/openid-connect/token",
                           {"grant_type": "password", "client_id": "admin-cli",
                            "username": "admin", "password": "admin"}, form=True)
    assert status == 200, body
    return body["access_token"]


def setup(tok):
    base = f"{KC}/admin/realms"
    assert http("POST", base, {"realm": REALM, "enabled": True}, tok)[0] == 201
    for role in ("raid-dumper", "decoy-admin"):
        assert http("POST", f"{base}/{REALM}/roles", {"name": role}, tok)[0] == 201

    clients = {
        CLIENT_ID: {"use.jwks.url": "true", "jwks.url": JWKS_URL_IN_CONTAINER},
        EMBEDDED_CLIENT_ID: {"use.jwks.url": "false",
                             "jwt.credential.certificate": open(os.path.join(WORK, "cert-c.b64")).read().strip()},
        # Several keys held on the client itself, with no URL to fetch.
        MULTI_KEY_CLIENT_ID: {"use.jwks.url": "false", "use.jwks.string": "true",
                              "jwks.string": json.dumps({"keys": [jwk_for("d"), jwk_for("e")]})},
        # Per-client access token lifespan, overriding the realm default.
        LIFESPAN_CLIENT_ID: {"use.jwks.url": "true", "jwks.url": JWKS_URL_IN_CONTAINER,
                             "access.token.lifespan": "300"},
    }
    dumper = http("GET", f"{base}/{REALM}/roles/raid-dumper", token=tok)[1]
    for cid, attrs in clients.items():
        status, _, headers = http("POST", f"{base}/{REALM}/clients", {
            "clientId": cid, "protocol": "openid-connect", "publicClient": False,
            "serviceAccountsEnabled": True, "standardFlowEnabled": False,
            "directAccessGrantsEnabled": False, "clientAuthenticatorType": "client-jwt",
            "attributes": {**attrs, "token.endpoint.auth.signing.alg": "RS256"},
        }, tok)
        assert status == 201, status
        internal_id = headers["Location"].rsplit("/", 1)[1]
        sa = http("GET", f"{base}/{REALM}/clients/{internal_id}/service-account-user", token=tok)[1]
        assert http("POST", f"{base}/{REALM}/users/{sa['id']}/role-mappings/realm", [dumper], tok)[0] == 204
        # Record what Keycloak actually stored for the authenticator.
        stored = http("GET", f"{base}/{REALM}/clients/{internal_id}", token=tok)[1]
        print(f"client {cid}: clientAuthenticatorType={stored['clientAuthenticatorType']} "
              f"secret_present={bool(stored.get('secret'))}", flush=True)


# ---------------------------------------------------------------- tests
def main():
    write_jwks(["a"])
    setup(admin_token())
    # T1 happy path, aud = issuer
    status, body, _ = token_request(sign_assertion("a", "a"))
    if record("T1 key A via JWKS URL, aud=issuer", True, status, body):
        roles, claims = roles_in(body["access_token"])
        print(f"     realm roles: {roles}; azp={claims.get('azp')}; scope={claims.get('scope')}", flush=True)
        results.append(("T1a token holds raid-dumper, not decoy-admin",
                        "PASS" if "raid-dumper" in roles and "decoy-admin" not in roles else "FAIL",
                        200, ", ".join(roles)))
        # The token must work as a plain bearer on the realm's userinfo-less endpoints; introspect not needed.

    # T2 aud = token endpoint
    status, body, _ = token_request(sign_assertion("a", "a", aud=TOKEN_URL))
    record("T2 aud=token endpoint", True, status, body)

    # T3 replayed jti
    jti = str(uuid.uuid4())
    first = sign_assertion("a", "a", jti=jti)
    token_request(first)
    status, body, _ = token_request(first)
    record("T3 replayed assertion (same jti) rejected", False, status, body)

    # T4 signed with a key not in the JWKS
    status, body, _ = token_request(sign_assertion("b", "b"))
    record("T4 unknown key B rejected", False, status, body)

    # T5 signed with key B but claiming kid A
    status, body, _ = token_request(sign_assertion("b", "a"))
    record("T5 key B forging kid A rejected", False, status, body)

    # T6 expired
    status, body, _ = token_request(sign_assertion("a", "a", exp_offset=-120))
    record("T6 expired assertion rejected", False, status, body)

    # T7 wrong audience
    status, body, _ = token_request(sign_assertion("a", "a", aud="https://attacker.example/realms/probe"))
    record("T7 wrong audience rejected", False, status, body)

    # T8 client_secret against a client-jwt client
    status, body, _ = token_request(secret="anything")
    record("T8 client_secret on client-jwt client rejected", False, status, body)

    # T9 no credentials at all
    status, body, _ = token_request()
    record("T9 no client authentication rejected", False, status, body)

    # T10 rotation: publish A+B, sign with B. Keycloak rate-limits JWKS refetches
    # (minTimeBetweenRequests, default 10s), and T4 just triggered a fetch.
    write_jwks(["a", "b"])
    time.sleep(12)
    status, body, _ = token_request(sign_assertion("b", "b"))
    record("T10 rotation: new key B picked up without restart", True, status, body)

    # T11 retire A: publish B only, sign with A. Observational: shows whether a
    # retired key keeps working from Keycloak's key cache.
    write_jwks(["b"])
    time.sleep(12)
    status, body, _ = token_request(sign_assertion("a", "a"))
    retired_ok = status == 200
    results.append(("T11 retired key A after JWKS removal (observational)", "INFO", status,
                    "still accepted (cached)" if retired_ok else "rejected"))
    print(f"[INFO] T11 retired key A: HTTP {status} -> {'still accepted (cached)' if retired_ok else 'rejected'}",
          flush=True)

    # T12 embedded-certificate alternative (no outbound fetch from Keycloak)
    # Keycloak keys an embedded certificate by its own derived kid, so an
    # arbitrary kid label finds no key even when the signature is valid.
    status, body, _ = token_request(sign_assertion("c", "c", client_id=EMBEDDED_CLIENT_ID), client_id=EMBEDDED_CLIENT_ID)
    record("T12 embedded certificate, arbitrary kid rejected", False, status, body)

    status, body, _ = token_request(sign_assertion("c", None, client_id=EMBEDDED_CLIENT_ID), client_id=EMBEDDED_CLIENT_ID)
    if record("T12b embedded certificate, no kid header", True, status, body):
        roles, _ = roles_in(body["access_token"])
        print(f"     realm roles: {roles}", flush=True)

    status, body, _ = token_request(sign_assertion("c", spki_thumbprint("c"), client_id=EMBEDDED_CLIENT_ID),
                                    client_id=EMBEDDED_CLIENT_ID)
    record("T12c embedded certificate, kid = SPKI SHA-256 thumbprint", True, status, body)

    status, body, _ = token_request(sign_assertion("a", None, client_id=EMBEDDED_CLIENT_ID), client_id=EMBEDDED_CLIENT_ID)
    record("T13 embedded client rejects key A", False, status, body)

    # T11b Authority-side eviction: an assertion with an unknown kid makes
    # Keycloak refetch the JWKS (now B only). If the refetch replaces the cached
    # key set, retired key A is then rejected with no agency action.
    time.sleep(12)
    status, body, _ = token_request(sign_assertion("b", "no-such-kid"))
    record("T11b unknown kid rejected (forces refetch)", False, status, body)
    status, body, _ = token_request(sign_assertion("a", "a"))
    record("T11c retired key A rejected after forced refetch", False, status, body)
    status, body, _ = token_request(sign_assertion("b", "b"))
    record("T11d current key B still accepted", True, status, body)

    # T15 several keys held on one client, no URL: both overlap keys work.
    for key in ("d", "e"):
        status, body, _ = token_request(sign_assertion(key, key, client_id=MULTI_KEY_CLIENT_ID),
                                        client_id=MULTI_KEY_CLIENT_ID)
        record(f"T15 multi-key client accepts key {key.upper()}", True, status, body)
    status, body, _ = token_request(sign_assertion("a", "a", client_id=MULTI_KEY_CLIENT_ID),
                                    client_id=MULTI_KEY_CLIENT_ID)
    record("T15c multi-key client rejects key A", False, status, body)

    # T16 per-client access token lifespan.
    status, body, _ = token_request(sign_assertion("b", "b", client_id=LIFESPAN_CLIENT_ID),
                                    client_id=LIFESPAN_CLIENT_ID)
    lifespan = body.get("expires_in") if isinstance(body, dict) else None
    results.append(("T16 per-client token lifespan of 300s honoured",
                    "PASS" if status == 200 and lifespan == 300 else "FAIL", status, f"expires_in={lifespan}"))
    print(f"[{results[-1][1]}] T16 per-client lifespan: HTTP {status} expires_in={lifespan}", flush=True)

    # T14 RA-side kill switch: disabling the client stops harvesting immediately,
    # even for a key still held in Keycloak's cache.
    tok = admin_token()
    base = f"{KC}/admin/realms/{REALM}/clients"
    client = http("GET", f"{base}?clientId={CLIENT_ID}", token=tok)[1][0]
    client["enabled"] = False
    assert http("PUT", f"{base}/{client['id']}", client, tok)[0] == 204
    status, body, _ = token_request(sign_assertion("b", "b"))
    record("T14 disabled client rejected (RA kill switch)", False, status, body)

    print("\n==== SUMMARY ====")
    for name, verdict, status, detail in results:
        print(f"{verdict:5} {status:4} {name} — {detail}")
    sys.exit(1 if any(v == "FAIL" for _, v, _, _ in results) else 0)


if __name__ == "__main__":
    main()
