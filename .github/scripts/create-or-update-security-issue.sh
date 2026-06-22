#!/usr/bin/env bash
# $1 = issue title (used for gh search), e.g. "Caiman :: Security: Dependency Vulnerabilities Detected"
# $2 = body heading suffix, e.g. "Vulnerabilities Detected"
set -euo pipefail

ISSUE_TITLE="$1"
BODY_HEADING="$2"

RUN_URL="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"
SCAN_DATE=$(TZ=UTC date '+%Y-%m-%d %H:%M:%S UTC')

{
  printf '## Security Scan Failed — %s\n\n' "$BODY_HEADING"
  printf '| Field | Value |\n'
  printf '|-------|-------|\n'
  printf '| **Workflow** | %s |\n' "$GITHUB_WORKFLOW"
  printf '| **Run** | [View run](%s) |\n' "$RUN_URL"
  printf '| **Date / Time (UTC)** | %s |\n\n' "$SCAN_DATE"
  printf '### Scan Output\n\n```\n'
  cat scan-output.txt
  printf '\n```\n'
} > issue-body.txt

EXISTING=$(gh issue list \
  --state open \
  --search "$ISSUE_TITLE" \
  --json number,title \
  | jq -r --arg t "$ISSUE_TITLE" '.[] | select(.title == $t) | .number' | head -1)

if [ -z "$EXISTING" ]; then
  gh issue create \
    --title "$ISSUE_TITLE" \
    --body-file issue-body.txt
else
  {
    printf '> **Recurring vulnerability report.** A security issue is already open — adding this scan result as a comment to keep the thread consolidated and avoid duplicate issues.\n\n---\n\n'
    cat issue-body.txt
  } > comment-body.txt
  gh issue comment "$EXISTING" --body-file comment-body.txt
fi
