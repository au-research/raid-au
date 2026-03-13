#!/usr/bin/env bash

JIRA_BASE_URL="https://ardc.atlassian.net"

if [ -z "$JIRA_EMAIL" ] || [ -z "$JIRA_API_TOKEN" ]; then
  echo "Error: JIRA_EMAIL and JIRA_API_TOKEN environment variables must be set"
  exit 1
fi

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
  echo "Usage: jira-update.sh <ticket-key> <field> <value>"
  echo "  ticket-key: e.g. RAID-123"
  echo "  field:      summary, description"
  echo "  value:      new value for the field"
  exit 1
fi

ticket_key="$1"
field="$2"
value="$3"

case "$field" in
  summary)
    payload=$(jq -n --arg val "$value" '{ fields: { summary: $val } }')
    ;;
  description)
    payload=$(jq -n --arg val "$value" '{
      fields: {
        description: {
          type: "doc",
          version: 1,
          content: [
            {
              type: "paragraph",
              content: [
                { type: "text", text: $val }
              ]
            }
          ]
        }
      }
    }')
    ;;
  *)
    echo "Error: unsupported field '$field'. Supported: summary, description"
    exit 1
    ;;
esac

response=$(curl -s -w "\n%{http_code}" \
  -X PUT \
  -H "Content-Type: application/json" \
  -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
  -d "$payload" \
  "${JIRA_BASE_URL}/rest/api/3/issue/${ticket_key}")

http_code=$(echo "$response" | tail -1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
  echo "Updated ${ticket_key} ${field}"
else
  echo "Error ($http_code): $(echo "$body" | jq -r '.errors // .errorMessages // .')"
  exit 1
fi
