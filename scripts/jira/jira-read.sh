#!/usr/bin/env bash

JIRA_BASE_URL="https://ardc.atlassian.net"

if [ -z "$JIRA_EMAIL" ] || [ -z "$JIRA_API_TOKEN" ]; then
  echo "Error: JIRA_EMAIL and JIRA_API_TOKEN environment variables must be set"
  exit 1
fi

if [ -z "$1" ]; then
  echo "Usage: jira-read.sh <ticket-key>"
  echo "  ticket-key: e.g. RAID-123"
  exit 1
fi

ticket_key="$1"

response=$(curl -s -w "\n%{http_code}" \
  -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
  "${JIRA_BASE_URL}/rest/api/3/issue/${ticket_key}")

http_code=$(echo "$response" | tail -1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
  echo "$body" | jq -r '
    "Key:         " + .key,
    "Summary:     " + .fields.summary,
    "Status:      " + .fields.status.name,
    "Type:        " + .fields.issuetype.name,
    "Assignee:    " + (.fields.assignee.displayName // "Unassigned"),
    "Priority:    " + (.fields.priority.name // "None"),
    "Parent:      " + (.fields.parent.key // "None"),
    "Created:     " + .fields.created,
    "Updated:     " + .fields.updated,
    "",
    "Description:",
    (if .fields.description then
      [.fields.description.content[]? | .. | .text? // empty] | join("")
    else
      "(no description)"
    end)'
else
  echo "Error ($http_code): $(echo "$body" | jq -r '.errors // .errorMessages // .')"
  exit 1
fi
