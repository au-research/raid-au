#!/usr/bin/env bash

JIRA_BASE_URL="https://ardc.atlassian.net"
PROJECT_KEY="RAID"

if [ -z "$JIRA_EMAIL" ] || [ -z "$JIRA_API_TOKEN" ]; then
  echo "Error: JIRA_EMAIL and JIRA_API_TOKEN environment variables must be set"
  exit 1
fi

if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Usage: jira-create.sh <issue-type> <summary> [description] [parent-key]"
  echo "  issue-type:  Story, Task, Bug, Subtask"
  echo "  summary:     ticket title"
  echo "  description: optional body text"
  echo "  parent-key:  required for Subtask (e.g. RAID-123)"
  exit 1
fi

issue_type="$1"
summary="$2"
description="${3:-}"
parent_key="${4:-}"

if [ "$issue_type" = "Subtask" ] && [ -z "$parent_key" ]; then
  echo "Error: parent-key is required for Subtask issue type"
  exit 1
fi

payload=$(jq -n \
  --arg project "$PROJECT_KEY" \
  --arg type "$issue_type" \
  --arg summary "$summary" \
  --arg desc "$description" \
  '{
    fields: {
      project: { key: $project },
      issuetype: { name: $type },
      summary: $summary,
      description: {
        type: "doc",
        version: 1,
        content: [
          {
            type: "paragraph",
            content: [
              { type: "text", text: $desc }
            ]
          }
        ]
      }
    }
  }')

# Remove description field if no description provided
if [ -z "$description" ]; then
  payload=$(echo "$payload" | jq 'del(.fields.description)')
fi

# Add parent field for subtasks
if [ -n "$parent_key" ]; then
  payload=$(echo "$payload" | jq --arg key "$parent_key" '.fields.parent = { key: $key }')
fi

response=$(curl -s -w "\n%{http_code}" \
  -X POST \
  -H "Content-Type: application/json" \
  -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
  -d "$payload" \
  "${JIRA_BASE_URL}/rest/api/3/issue")

http_code=$(echo "$response" | tail -1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
  ticket_key=$(echo "$body" | jq -r '.key')
  echo "$ticket_key"
else
  echo "Error ($http_code): $(echo "$body" | jq -r '.errors // .errorMessages // .')"
  exit 1
fi
