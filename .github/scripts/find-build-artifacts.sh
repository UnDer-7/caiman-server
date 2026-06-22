#!/usr/bin/env bash
set -euo pipefail

JAR_FILE=$(find caiman-app/build/libs -name "*.jar" ! -name "*-plain.jar" -type f | head -n 1)
NATIVE_FILE="caiman-app/build/native/nativeCompile/caiman-app"

if [ -z "$JAR_FILE" ]; then
  echo "::error::JAR file not found in caiman-app/build/libs/"
  exit 1
fi

if [ ! -f "$NATIVE_FILE" ]; then
  echo "::error::Native binary not found at $NATIVE_FILE"
  exit 1
fi

echo "jar_file=$JAR_FILE" >> "$GITHUB_OUTPUT"
echo "native_file=$NATIVE_FILE" >> "$GITHUB_OUTPUT"
echo "Found JAR: $JAR_FILE"
echo "Found native binary: $NATIVE_FILE"
