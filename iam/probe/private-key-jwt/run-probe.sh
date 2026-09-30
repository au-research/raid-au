#!/usr/bin/env bash
# RAID-836: isolated Keycloak 26.6.2 probe for private_key_jwt client authentication.
# Starts a throwaway Keycloak (dev mode, H2) and a local JWKS server, runs probe.py,
# then tears both down. Touches nothing outside this directory and the one container.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="$HERE/work"
CONTAINER=raid836-kc-probe
KC_PORT=8095
JWKS_PORT=8765
IMAGE=quay.io/keycloak/keycloak:26.6.2

cleanup() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
  [[ -n "${JWKS_PID:-}" ]] && kill "$JWKS_PID" 2>/dev/null || true
}
trap cleanup EXIT

rm -rf "$WORK" && mkdir -p "$WORK/jwks"

# Keys A and B: JWKS-published. Key C: embedded certificate. Keys D and E: multi-key client.
for k in a b c d e; do
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$WORK/key-$k.pem" 2>/dev/null
done
openssl req -x509 -new -key "$WORK/key-c.pem" -subj "/CN=raid-federation-harvester" -days 30 \
  -outform DER -out "$WORK/cert-c.der" 2>/dev/null
base64 < "$WORK/cert-c.der" | tr -d '\n' > "$WORK/cert-c.b64"

echo '{"keys":[]}' > "$WORK/jwks/jwks.json"
python3 -m http.server "$JWKS_PORT" --bind 0.0.0.0 --directory "$WORK/jwks" >"$WORK/jwks-server.log" 2>&1 &
JWKS_PID=$!

docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
docker run -d --name "$CONTAINER" -p "$KC_PORT:8080" \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  "$IMAGE" start-dev >/dev/null

echo "Waiting for Keycloak on :$KC_PORT ..."
for _ in $(seq 1 90); do
  if curl -sf "http://localhost:$KC_PORT/realms/master" >/dev/null; then break; fi
  sleep 2
done
curl -sf "http://localhost:$KC_PORT/realms/master" >/dev/null || { docker logs "$CONTAINER" | tail -50; exit 1; }
echo "Keycloak up: $(docker inspect -f '{{.Config.Image}}' "$CONTAINER")"

set +e
WORK_DIR="$WORK" KC_URL="http://localhost:$KC_PORT" \
  JWKS_URL_IN_CONTAINER="http://host.docker.internal:$JWKS_PORT/jwks.json" \
  python3 "$HERE/probe.py"
rc=$?
set -e

echo
echo "==== JWKS fetches seen by the JWKS server ===="
grep -c "GET /jwks.json" "$WORK/jwks-server.log" || true
echo "==== Keycloak client-auth log lines ===="
docker logs "$CONTAINER" 2>&1 | grep -iE "jwks|client.?assertion|invalid_client|signature" | tail -30 || true
exit $rc
