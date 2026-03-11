#!/usr/bin/env bash

JIRA_BASE_URL="https://ardc.atlassian.net"

if [ -z "$JIRA_EMAIL" ] || [ -z "$JIRA_API_TOKEN" ]; then
  echo "Error: JIRA_EMAIL and JIRA_API_TOKEN environment variables must be set"
  exit 1
fi

if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Usage: jira-transition.sh <ticket-key> <status>"
  echo "  ticket-key: e.g. RAID-123"
  echo "  status:     todo, in-progress, done"
  exit 1
fi

ticket_key="$1"
status="$2"

case "$status" in
  todo)        target_name="To Do" ;;
  in-progress) target_name="In Progress" ;;
  done)        target_name="Done" ;;
  *)
    echo "Error: unsupported status '$status'. Supported: todo, in-progress, done"
    exit 1
    ;;
esac

# Fetch available transitions for this ticket
transitions_response=$(curl -s -w "\n%{http_code}" \
  -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
  "${JIRA_BASE_URL}/rest/api/3/issue/${ticket_key}/transitions")

http_code=$(echo "$transitions_response" | tail -1)
body=$(echo "$transitions_response" | sed '$d')

if [ "$http_code" -lt 200 ] || [ "$http_code" -ge 300 ]; then
  echo "Error fetching transitions ($http_code): $(echo "$body" | jq -r '.errors // .errorMessages // .')"
  exit 1
fi

# Find the transition ID matching the target status
transition_id=$(echo "$body" | jq -r --arg name "$target_name" \
  '.transitions[] | select(.name == $name) | .id')

if [ -z "$transition_id" ]; then
  available=$(echo "$body" | jq -r '[.transitions[].name] | join(", ")')
  echo "Error: transition '${target_name}' not available for ${ticket_key}"
  echo "Available transitions: ${available}"
  exit 1
fi

# Execute the transition
response=$(curl -s -w "\n%{http_code}" \
  -X POST \
  -H "Content-Type: application/json" \
  -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
  -d "{\"transition\": {\"id\": \"${transition_id}\"}}" \
  "${JIRA_BASE_URL}/rest/api/3/issue/${ticket_key}/transitions")

http_code=$(echo "$response" | tail -1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
  echo "Transitioned ${ticket_key} to ${target_name}"
else
  echo "Error ($http_code): $(echo "$body" | jq -r '.errors // .errorMessages // .')"
  exit 1
fi
