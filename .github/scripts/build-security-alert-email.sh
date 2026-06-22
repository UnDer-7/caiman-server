#!/usr/bin/env bash
# $1 = alert title suffix, e.g. "Vulnerabilities Detected" or "Base Image Vulnerabilities Detected"
set -euo pipefail

ALERT_TITLE="$1"

RUN_URL="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"
SCAN_DATE=$(TZ=UTC date '+%Y-%m-%d %H:%M:%S UTC')

{
  printf 'Security Scan Failed — %s\n\n' "$ALERT_TITLE"
  printf 'Workflow:  %s\n' "$GITHUB_WORKFLOW"
  printf 'Run:       %s\n' "$RUN_URL"
  printf 'Date/Time: %s\n' "$SCAN_DATE"
  printf '\n--- Scan Output ---\n\n'
  cat scan-output.txt
} > email-body.txt

cat > email-body.html << 'HTMLEOF'
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: Arial, sans-serif; color: #333; margin: 0; padding: 0; }
    .header { background-color: #b71c1c; color: #fff; padding: 20px 24px; }
    .header h2 { margin: 0; font-size: 20px; }
    .content { padding: 24px; }
    .field { margin-bottom: 12px; }
    .label { font-weight: bold; color: #555; }
    .value { margin-left: 8px; }
    a { color: #1565c0; }
    .divider { border: none; border-top: 1px solid #ddd; margin: 20px 0; }
    .output-title { font-weight: bold; color: #555; margin-bottom: 8px; }
    pre {
      background: #f5f5f5;
      border-left: 4px solid #b71c1c;
      padding: 16px;
      font-size: 12px;
      font-family: 'Courier New', monospace;
      overflow: auto;
      white-space: pre;
      margin: 0;
    }
  </style>
</head>
<body>
  <div class="header">
HTMLEOF

printf '    <h2>Security Scan Failed &mdash; %s</h2>\n' "$ALERT_TITLE" >> email-body.html

cat >> email-body.html << 'HTMLEOF'
  </div>
  <div class="content">
    <div class="field">
      <span class="label">Workflow:</span>
HTMLEOF

printf '      <span class="value">%s</span>\n    </div>\n' "$GITHUB_WORKFLOW" >> email-body.html
printf '    <div class="field"><span class="label">Run:</span> <span class="value"><a href="%s">%s</a></span></div>\n' "$RUN_URL" "$RUN_URL" >> email-body.html
printf '    <div class="field"><span class="label">Date / Time:</span> <span class="value">%s</span></div>\n' "$SCAN_DATE" >> email-body.html

cat >> email-body.html << 'HTMLEOF'
    <hr class="divider">
    <div class="output-title">Scan Output</div>
    <pre>
HTMLEOF

sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g' scan-output.txt >> email-body.html

cat >> email-body.html << 'HTMLEOF'
    </pre>
  </div>
</body>
</html>
HTMLEOF
