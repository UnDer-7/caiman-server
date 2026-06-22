#!/usr/bin/env bash
# Validates that CURRENT_VERSION is strictly greater than the latest remote tag
# and that the exact tag does not already exist.
set -euo pipefail

CURRENT_VERSION="$1"
CURRENT_SEMVER="${CURRENT_VERSION#v}"

LATEST_TAG=$(git ls-remote --tags origin | \
  grep -oP 'refs/tags/\Kv[0-9]+\.[0-9]+\.[0-9]+$' | \
  sort -V | \
  tail -n 1)

if [ -z "$LATEST_TAG" ]; then
  LATEST_TAG="0.0.0"
  echo "No existing version tags found. Starting from 0.0.0"
else
  echo "Latest published version: $LATEST_TAG"
fi

LATEST_SEMVER="${LATEST_TAG#v}"

readonly VERSION_COMPARISON_GREATER="greater"
readonly VERSION_COMPARISON_LESS="less"
readonly VERSION_COMPARISON_EQUAL="equal"

compare_versions() {
  if [ "$1" = "$2" ]; then echo $VERSION_COMPARISON_EQUAL; return; fi

  IFS='.' read -r -a current <<< "$1"
  IFS='.' read -r -a latest <<< "$2"

  if   [ "${current[0]}" -gt "${latest[0]}" ]; then echo $VERSION_COMPARISON_GREATER; return
  elif [ "${current[0]}" -lt "${latest[0]}" ]; then echo $VERSION_COMPARISON_LESS;    return; fi

  if   [ "${current[1]}" -gt "${latest[1]}" ]; then echo $VERSION_COMPARISON_GREATER; return
  elif [ "${current[1]}" -lt "${latest[1]}" ]; then echo $VERSION_COMPARISON_LESS;    return; fi

  if   [ "${current[2]}" -gt "${latest[2]}" ]; then echo $VERSION_COMPARISON_GREATER; return
  elif [ "${current[2]}" -lt "${latest[2]}" ]; then echo $VERSION_COMPARISON_LESS;    return; fi

  echo $VERSION_COMPARISON_EQUAL
}

COMPARISON=$(compare_versions "$CURRENT_SEMVER" "$LATEST_SEMVER")

if [ "$COMPARISON" != "$VERSION_COMPARISON_GREATER" ]; then
  echo "::error::Version validation failed!"
  echo ""

  if [ "$COMPARISON" = "$VERSION_COMPARISON_EQUAL" ]; then
    echo "❌ Current version $CURRENT_VERSION is equal to the latest published version."
  else
    echo "❌ Current version $CURRENT_VERSION is LESS than the latest published version $LATEST_TAG."
  fi

  echo ""
  echo "💡 You must bump the version to be greater than $LATEST_TAG"
  echo ""
  echo "Following Semantic Versioning:"
  echo "  - Patch release (bug fixes): just version-set v<patch+1>"
  echo "  - Minor release (new features): just version-set v<minor+1>.0"
  echo "  - Major release (breaking changes): just version-set v<major+1>.0.0"
  echo ""
  echo "After updating the version, commit and push the changes."
  exit 1
fi

echo "✅ Version $CURRENT_VERSION is greater than $LATEST_TAG"

if git ls-remote --tags origin | grep -q "refs/tags/$CURRENT_VERSION$"; then
  echo "::error::Tag $CURRENT_VERSION already exists in the repository!"
  echo ""
  echo "❌ A release for version $CURRENT_VERSION has already been created."
  echo ""
  echo "💡 This should not happen if the pull request pipeline is working correctly."
  echo "   The version validation should have prevented this."
  echo ""
  echo "Possible causes:"
  echo "  1. The tag was created manually"
  echo "  2. A previous deploy workflow run already created this tag"
  echo "  3. The pull request pipeline was bypassed"
  exit 1
fi

echo "✅ Tag $CURRENT_VERSION does not exist, proceeding"
