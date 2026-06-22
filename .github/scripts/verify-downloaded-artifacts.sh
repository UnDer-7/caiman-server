#!/usr/bin/env bash
set -euo pipefail

echo "Downloaded artifacts:"

JAR_FILE=$(find caiman-app/build/libs -name "*.jar" ! -name "*-plain.jar" -type f | head -n 1)
NATIVE_FILE="caiman-app/build/native/nativeCompile/caiman-app"

if [ -z "$JAR_FILE" ]; then
  echo "::error::JAR file not found!"
  exit 1
fi

if [ ! -f "$NATIVE_FILE" ]; then
  echo "::error::Native binary not found!"
  exit 1
fi

chmod +x "$NATIVE_FILE"

echo "✅ JAR file: $JAR_FILE"
echo "✅ Native binary: $NATIVE_FILE"
