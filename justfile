# ====================================================================================
# VARIABLES
# ====================================================================================

project-name    := "caiman-server"
registry-host   := "under7"
project-version := `grep -oP '(?<=version = ")[^"]*' build.gradle.kts`

# ====================================================================================
# DEFAULT
# ====================================================================================

[private]
default:
    @just --list

# ====================================================================================
# ALIASES
# ====================================================================================
alias c     := dev-compile
alias t     := test-coverage
alias tn    := test-integration-native
alias f     := fmt-apply

# ====================================================================================
# DEV
# ====================================================================================

# Clean all build outputs
[group: "dev"]
dev-clean:
    ./gradlew clean --rerun-tasks

# Compile the application
[group: "dev"]
dev-compile:
    @echo ">>> Compiling..."
    ./gradlew clean compileJava --rerun-tasks

# Build and run the application locally (loads .env variables)
[group: "dev"]
dev-run:
    #!/usr/bin/env bash
    set -eo pipefail
    echo ">>> Loading .env and starting application..."
    set -a
    source .env
    set +a
    ./gradlew :caiman-app:bootRun

# ====================================================================================
# TEST
# ====================================================================================

# Run unit tests across all modules
[group: "test"]
test-unit:
    ./gradlew clean unitTest --rerun-tasks

# Run integration tests on JVM across all modules
[group: "test"]
test-integration-jvm:
    ./gradlew clean integrationTestJvm --rerun-tasks

# Run integration tests in GraalVM native mode (requires Docker)
[group: "test"]
test-integration-native:
    ./gradlew :caiman-app:integrationTestNative --rerun-tasks

# Run unit and integration tests (JVM) across all modules
[group: "test"]
test:
    ./gradlew clean unitTest integrationTestJvm --rerun-tasks

# Run all tests (JVM) and generate aggregated JaCoCo report
[group: "test"]
test-coverage:
    ./gradlew clean unitTest integrationTestJvm jacocoRootReport --rerun-tasks

# ====================================================================================
# BUILD
# ====================================================================================

# Build GraalVM native image + bootJar in a single Gradle invocation (loads .env variables)
[group: "build"]
build-artifacts:
    #!/usr/bin/env bash
    set -eo pipefail
    START=$(date +%s)
    echo 'Loading .env and building native image + bootJar...'
    set -a
    source .env
    set +a
    ./gradlew clean :caiman-app:nativeCompile :caiman-app:bootJar -x test --rerun-tasks
    END=$(date +%s)
    ELAPSED=$((END-START))
    echo "Artifacts built in $((ELAPSED/3600))h $(((ELAPSED%3600)/60))m $((ELAPSED%60))s"

# Build the bootJar (loads .env variables, run before build-jar-docker if code changed)
[group: "build"]
build-jar:
    #!/usr/bin/env bash
    set -eo pipefail
    START=$(date +%s)
    echo 'Loading .env and building bootJar...'
    set -a
    source .env
    set +a
    ./gradlew :caiman-app:bootJar -x test --rerun-tasks
    END=$(date +%s)
    ELAPSED=$((END-START))
    echo "bootJar built in $((ELAPSED/3600))h $(((ELAPSED%3600)/60))m $((ELAPSED%60))s"

# Build JVM Docker image from Dockerfile.jvm (run build-jar first)
[group: "build"]
build-jar-docker:
    #!/usr/bin/env bash
    set -euo pipefail
    START=$(date +%s)
    echo "Building Docker JVM image..."
    docker build -f Dockerfile.jvm \
        -t {{registry-host}}/{{project-name}}:{{project-version}}-jvm .
    END=$(date +%s)
    ELAPSED=$((END-START))
    echo "Docker JVM build completed in $((ELAPSED/3600))h $(((ELAPSED%3600)/60))m $((ELAPSED%60))s"

# Build GraalVM native image (loads .env variables)
[group: "build"]
build-native:
    #!/usr/bin/env bash
    set -eo pipefail
    START=$(date +%s)
    echo 'Loading .env and building native image...'
    set -a
    source .env
    set +a
    ./gradlew :caiman-app:nativeCompile -x test --rerun-tasks
    END=$(date +%s)
    ELAPSED=$((END-START))
    echo "Native build completed in $((ELAPSED/3600))h $(((ELAPSED%3600)/60))m $((ELAPSED%60))s"

# Build native Docker image from Dockerfile.native (run build-native first)
[group: "build"]
build-native-docker:
    #!/usr/bin/env bash
    set -euo pipefail
    START=$(date +%s)
    echo "Building Docker native image..."
    docker build -f Dockerfile.native \
        -t {{registry-host}}/{{project-name}}:{{project-version}}-native .
    END=$(date +%s)
    ELAPSED=$((END-START))
    echo "Docker native build completed in $((ELAPSED/3600))h $(((ELAPSED%3600)/60))m $((ELAPSED%60))s"

# ====================================================================================
# VERSION
# ====================================================================================

# Show current project version
[group: "version"]
version:
    @echo {{project-version}}

# Set project version (usage: just version-set 0.1.2)
[group: "version"]
version-set new-version:
    @sed -i 's/version = "[^"]*"/version = "{{new-version}}"/' build.gradle.kts
    @echo "Version {{project-version}} → {{new-version}}"

# ====================================================================================
# CODING STYLE
# ====================================================================================

# Check code formatting (fails if any file needs reformatting)
[group: "fmt"]
fmt-check:
    ./gradlew spotlessCheck --rerun-tasks

# Apply code formatting to all source files
[group: "fmt"]
fmt-apply:
    ./gradlew spotlessApply --rerun-tasks

# ====================================================================================
# ANALYSIS
# ====================================================================================

# Publish analysis to SonarCloud (run 'test-coverage' first, requires SONAR_TOKEN env var)
[group: "analysis"]
sonar:
    #!/usr/bin/env bash
    set -euo pipefail
    echo ">>> Publishing analysis to SonarCloud..."
    if [ -z "${SONAR_TOKEN:-}" ]; then
        echo "ERROR: SONAR_TOKEN environment variable is not set"
        echo "Set it: export SONAR_TOKEN=your_token"
        exit 1
    fi
    if [ ! -f "build/reports/jacoco/aggregate/jacoco.xml" ]; then
        echo "ERROR: JaCoCo coverage report not found"
        echo "Run 'just test-coverage' first to generate coverage data"
        exit 1
    fi
    ./gradlew sonar -Dsonar.token="$SONAR_TOKEN" --rerun-tasks
    echo ">>> Analysis published!"
    echo ">>> https://sonarcloud.io/dashboard?id=UnDer-7_caiman-server"

# ====================================================================================
# SECURITY
# ====================================================================================

# Regenerate Gradle dependency lockfiles (run after any dependency change, then commit)
[group: "security"]
security-update-locks:
    ./gradlew updateDependencyLocks --write-locks -q
    @echo ">>> Lockfiles updated. Commit the gradle.lockfile changes."

# Generate SBOM and scan Java dependencies + GitHub Actions workflows
[group: "security"]
security-scan-deps:
    #!/usr/bin/env bash
    set -euo pipefail
    for cmd in syft grype; do
        if ! command -v "$cmd" >/dev/null 2>&1; then
            echo "ERROR: $cmd not found in PATH"
            echo "Install Syft: https://oss.anchore.com/docs/installation/syft/#installer-script"
            echo "Install Grype: https://oss.anchore.com/docs/installation/grype/#installer-script"
            exit 1
        fi
    done
    echo ">>> Generating SBOM..."
    syft scan dir:. -o syft-json=sbom.json \
        --source-name "{{project-name}}" \
        --source-version "{{project-version}}" \
        --exclude './.gradle' \
        --exclude './build' \
        --exclude './**/build'
    echo ">>> Scanning dependencies..."
    grype sbom:./sbom.json --fail-on high

# Scan base Docker images for known vulnerabilities
[group: "security"]
security-scan-base-images:
    #!/usr/bin/env bash
    set -euo pipefail
    if ! command -v grype >/dev/null 2>&1; then
        echo "ERROR: grype not found in PATH"
        echo "Install: curl -sSfL https://raw.githubusercontent.com/anchore/grype/main/install.sh | sh -s -- -b /usr/local/bin"
        exit 1
    fi
    JVM_IMAGE=$(grep "^FROM" Dockerfile.jvm | awk 'NR==1{print $2}')
    NATIVE_IMAGE=$(grep "^FROM" Dockerfile.native | awk 'NR==1{print $2}')
    PG_IMAGE=$(grep "image: postgres" local/docker-compose.yml | awk 'NR==1{print $2}')
    echo ">>> Scanning JVM base image (Dockerfile.jvm)..."
    grype registry:"$JVM_IMAGE" --fail-on high
    echo ">>> Scanning native base image (Dockerfile.native)..."
    grype registry:"$NATIVE_IMAGE" --fail-on high
    echo ">>> Scanning PostgreSQL image (local/docker-compose.yml)..."
    grype "$PG_IMAGE" --fail-on high
