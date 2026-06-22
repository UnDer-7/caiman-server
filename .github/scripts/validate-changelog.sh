#!/usr/bin/env bash
set -euo pipefail

CURRENT_VERSION="$1"
CHANGELOG_FILE="CHANGELOG.md"

if [ ! -f "$CHANGELOG_FILE" ]; then
  echo "::error::CHANGELOG validation failed!"
  echo ""
  echo "❌ CHANGELOG.md file does not exist in the repository."
  echo ""
  echo "💡 Create a CHANGELOG.md file in the root of the project."
  echo ""
  echo "📖 Follow the Keep a Changelog format: https://keepachangelog.com/"
  echo ""
  echo "After updating CHANGELOG.md, commit and push the changes."
  exit 1
fi

ESCAPED_VERSION=$(printf '%s' "$CURRENT_VERSION" | sed 's/\./\\./g')
if ! grep -q "^## \[$ESCAPED_VERSION\]" "$CHANGELOG_FILE"; then
  echo "::error::CHANGELOG validation failed!"
  echo ""
  echo "❌ Version $CURRENT_VERSION is not documented in CHANGELOG.md."
  echo ""
  echo "💡 Add a version header to CHANGELOG.md:"
  echo "   ## [$CURRENT_VERSION] - $(date +%Y-%m-%d)"
  echo ""
  echo "Example structure:"
  echo "   ## [$CURRENT_VERSION] - $(date +%Y-%m-%d)"
  echo "   "
  echo "   ### Added"
  echo "   "
  echo "   #### Feature Name"
  echo "   - Description of changes"
  echo ""
  echo "📖 Follow the Keep a Changelog format: https://keepachangelog.com/"
  echo ""
  echo "After updating CHANGELOG.md, commit and push the changes."
  exit 1
fi

VERSION_SECTION=$(awk -v ver="## [$CURRENT_VERSION]" '
  index($0, ver) == 1 { found=1; next }
  /^## \[/ && found { exit }
  found { print }
' "$CHANGELOG_FILE")

if ! echo "$VERSION_SECTION" | grep -q "^####"; then
  echo "::error::CHANGELOG validation failed!"
  echo ""
  echo "❌ Version $CURRENT_VERSION exists in CHANGELOG.md but has no documented changes."
  echo ""
  echo "💡 Add at least one change description using #### headers:"
  echo ""
  echo "Example:"
  echo "   ## [$CURRENT_VERSION] - $(date +%Y-%m-%d)"
  echo "   "
  echo "   ### Added"
  echo "   "
  echo "   #### New Feature API"
  echo "   - \`POST /api/new-endpoint\` - Description of the endpoint"
  echo ""
  echo "📖 Follow the Keep a Changelog format: https://keepachangelog.com/"
  echo ""
  echo "After updating CHANGELOG.md, commit and push the changes."
  exit 1
fi

echo "✅ CHANGELOG.md properly documented for version $CURRENT_VERSION"
