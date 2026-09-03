#!/usr/bin/env bash
# Configure the ORCID identity provider and its attribute mappers in a RAiD Keycloak realm.
#
# Fixes: ORCID iD appearing in the First Name field (and as the username) after
# a user signs in with ORCID for the first time.
#
# Usage:
#   KC_URL=http://localhost:8001 KC_ADMIN=admin KC_ADMIN_PASSWORD=admin \
#   ORCID_CLIENT_ID=APP-XXXXXXXXXXXXXXXX ORCID_CLIENT_SECRET=<secret> \
#   ./configure-orcid-idp.sh
set -euo pipefail

KC_URL="${KC_URL:-http://localhost:8001}"
KC_REALM="${KC_REALM:-raid}"
KC_ADMIN="${KC_ADMIN:-admin}"
KC_ADMIN_PASSWORD="${KC_ADMIN_PASSWORD:-admin}"
ORCID_BASE="${ORCID_BASE:-https://sandbox.orcid.org}"
ORCID_CLIENT_ID="${ORCID_CLIENT_ID:-REPLACE_WITH_ORCID_CLIENT_ID}"
ORCID_CLIENT_SECRET="${ORCID_CLIENT_SECRET:-REPLACE_WITH_ORCID_CLIENT_SECRET}"
IDP_ALIAS="${IDP_ALIAS:-orcid}"

echo "==> Authenticating to ${KC_URL} as ${KC_ADMIN}"
TOKEN=$(curl -sf -X POST "${KC_URL}/realms/master/protocol/openid-connect/token" \
  -d "client_id=admin-cli" -d "username=${KC_ADMIN}" \
  -d "password=${KC_ADMIN_PASSWORD}" -d "grant_type=password" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

api() { # method path [body]
  local method="$1" path="$2" body="${3:-}"
  if [ -n "$body" ]; then
    curl -sf -X "$method" "${KC_URL}/admin/realms/${KC_REALM}${path}" \
      -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" -d "$body"
  else
    curl -sf -X "$method" "${KC_URL}/admin/realms/${KC_REALM}${path}" \
      -H "Authorization: Bearer ${TOKEN}"
  fi
}

echo "==> Creating/updating '${IDP_ALIAS}' identity provider"
IDP_BODY=$(cat <<JSON
{
  "alias": "${IDP_ALIAS}",
  "displayName": "ORCID",
  "providerId": "oidc",
  "enabled": true,
  "trustEmail": false,
  "storeToken": false,
  "addReadTokenRoleOnCreate": false,
  "linkOnly": false,
  "firstBrokerLoginFlowAlias": "first broker login",
  "config": {
    "issuer": "${ORCID_BASE}",
    "authorizationUrl": "${ORCID_BASE}/oauth/authorize",
    "tokenUrl": "${ORCID_BASE}/oauth/token",
    "userInfoUrl": "${ORCID_BASE}/oauth/userinfo",
    "jwksUrl": "${ORCID_BASE}/oauth/jwks",
    "useJwksUrl": "true",
    "validateSignature": "true",
    "clientId": "${ORCID_CLIENT_ID}",
    "clientSecret": "${ORCID_CLIENT_SECRET}",
    "clientAuthMethod": "client_secret_post",
    "defaultScope": "openid",
    "disableUserInfo": "false",
    "syncMode": "FORCE",
    "pkceEnabled": "false"
  }
}
JSON
)

if api GET "/identity-provider/instances/${IDP_ALIAS}" >/dev/null 2>&1; then
  echo "    exists - updating"
  api PUT "/identity-provider/instances/${IDP_ALIAS}" "${IDP_BODY}" >/dev/null
else
  echo "    creating"
  api POST "/identity-provider/instances" "${IDP_BODY}" >/dev/null
fi

# Remove any existing mappers so this script is idempotent.
echo "==> Clearing existing mappers on '${IDP_ALIAS}'"
EXISTING=$(api GET "/identity-provider/instances/${IDP_ALIAS}/mappers")
echo "${EXISTING}" | python3 -c "
import sys,json
for m in json.load(sys.stdin):
    print(m['id'])
" | while read -r id; do
  [ -n "$id" ] && api DELETE "/identity-provider/instances/${IDP_ALIAS}/mappers/${id}" >/dev/null
done

add_attr_mapper() { # name claim user-attribute
  local name="$1" claim="$2" attr="$3"
  echo "    + ${name}: claim '${claim}' -> ${attr}"
  api POST "/identity-provider/instances/${IDP_ALIAS}/mappers" "$(cat <<JSON
{
  "name": "${name}",
  "identityProviderAlias": "${IDP_ALIAS}",
  "identityProviderMapper": "oidc-user-attribute-idp-mapper",
  "config": {
    "syncMode": "FORCE",
    "claim": "${claim}",
    "user.attribute": "${attr}"
  }
}
JSON
)" >/dev/null
}

echo "==> Adding attribute mappers"
add_attr_mapper "orcid-given-name"  "given_name"  "firstName"
add_attr_mapper "orcid-family-name" "family_name" "lastName"
# Keep the ORCID iD, but as its own attribute rather than as the display name.
add_attr_mapper "orcid-id"          "sub"         "orcid"

echo "==> Verifying"
api GET "/identity-provider/instances/${IDP_ALIAS}/mappers" | python3 -c "
import sys,json
ms=json.load(sys.stdin)
print(f'{len(ms)} mapper(s) configured:')
for m in sorted(ms,key=lambda x:x['name']):
    c=m['config']
    print(f\"  {m['name']:<20} {m['identityProviderMapper']:<32} claim={c.get('claim'):<12} -> {c.get('user.attribute')}  syncMode={c.get('syncMode')}\")
"
echo "==> Done"
