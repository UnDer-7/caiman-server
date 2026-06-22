#!/usr/bin/env bash
set -euo pipefail

VERSION="$1"
CHANGELOG_FILE="CHANGELOG.md"

CHANGELOG_CONTENT=$(awk -v ver="## [$VERSION]" '
  index($0, ver) == 1 { found=1; next }
  /^## \[/ && found { exit }
  found { print }
' "$CHANGELOG_FILE")

JAR_NAME="caiman-app-${VERSION}.jar"
NATIVE_NAME="caiman-app-${VERSION}-linux-amd64"

printf '%s\n' "${CHANGELOG_CONTENT}" > changelog_body.txt

cat >> changelog_body.txt << EOF

## Docker Images

> For details on available tags and how to choose between JVM and native, see the (Docker Images guide - TODO).

| Flavor | Image |
|--------|-------|
| Native (default) | \`under7/caiman-server:${VERSION}-native\` |
| JVM | \`under7/caiman-server:${VERSION}-jvm\` |

[View all tags on Docker Hub](https://hub.docker.com/r/under7/caiman-server/tags?name=${VERSION})

## Assets

> Not sure which file to download? See the (installation guide - TODO).

| Asset | Description |
|-------|-------------|
| \`${JAR_NAME}\` | JVM executable — requires Java 25+ (JRE) on the host. Runs on any OS. |
| \`${JAR_NAME}.bundle\` | Cosign signature bundle for the JAR — verify with \`cosign verify-blob\`. |
| \`${NATIVE_NAME}\` | Native binary for Linux x86-64 — no JVM required, runs standalone. Faster startup, lower memory. |
| \`${NATIVE_NAME}.bundle\` | Cosign signature bundle for the native binary — verify with \`cosign verify-blob\`. |
| \`sbom.json\` | Software Bill of Materials (CycloneDX) — full list of dependencies included in this release. |
| \`sbom.json.bundle\` | Cosign signature bundle for the SBOM — verify with \`cosign verify-blob\`. |
EOF

echo "✅ Extracted CHANGELOG for version $VERSION"
