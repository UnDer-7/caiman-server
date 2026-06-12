# Dockerfile JVM Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Buildpack `bootBuildImage` with `Dockerfile.jvm` using layered JAR extraction, Java 25 AOT Cache training baked into the image, and explicit JVM flags for container-aware memory management.

**Architecture:** Three-stage multi-stage Dockerfile at project root. Stage 1 (builder) extracts the bootJar into 4 Docker layers via `jarmode=tools`. Stage 2 (aot-trainer) runs a full Spring context training run, writing `app.aot` — training env vars are isolated to this stage and never leak to runtime. Stage 3 (runtime) assembles layers + `app.aot`, sets JVM flags via ENTRYPOINT. Makefile gets two separate targets: `build/jar` (Gradle only) and `build/docker-jvm` (Docker build only).

**Tech Stack:** Docker multi-stage build, BellSoft Liberica OpenJRE 25 (`bellsoft/liberica-openjre-debian:25-cds`), Spring Boot 4.0.6 `jarmode=tools`, `org.springframework.boot.loader.launch.JarLauncher`, Java 25 `-XX:AOTCache`, ZGC, Spring virtual threads.

---

### Task 1: Remove bootBuildImage config from caiman-app/build.gradle.kts

**Files:**
- Modify: `caiman-app/build.gradle.kts`

- [ ] **Step 1: Remove the BootBuildImage import (line 2)**

Current file starts with:
```kotlin
import org.apache.tools.ant.filters.ReplaceTokens
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
```

Remove line 2. After removal:
```kotlin
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
```

- [ ] **Step 2: Remove the bootBuildImage task block (lines 58-66 at end of file)**

Remove the entire block at end of file:
```kotlin
tasks.named<BootBuildImage>("bootBuildImage") {
    imageName.set("under7/caiman-server:${project.version}-jvm")
    publish.set(false)
    environment.putAll(mapOf(
        "BP_JVM_VERSION" to "25",
        "BP_JVM_TYPE" to "JRE",
        "BPL_JVM_THREAD_COUNT" to "50",
    ))
}
```

After removal, the file ends with the closing `}` of the `dependencies { }` block.

- [ ] **Step 3: Verify Gradle compiles cleanly**

```bash
./gradlew :caiman-app:compileJava -q 2>&1 | tail -5
```

Expected: no output or `BUILD SUCCESSFUL`. No `Unresolved reference: BootBuildImage` error.

---

### Task 2: Create Dockerfile.jvm

**Files:**
- Create: `Dockerfile.jvm` (project root `/home/under7/Workspace/caiman/caiman-server/`)

**Key fact confirmed from MANIFEST.MF:** Spring Boot 4.0.6 launcher class is `org.springframework.boot.loader.launch.JarLauncher` (note `launch` sub-package — different from Spring Boot 3's `org.springframework.boot.loader.JarLauncher`).

- [ ] **Step 1: Create Dockerfile.jvm**

```dockerfile
# ============================================================
# Stage 1: Extract bootJar into layers for Docker cache efficiency
# Layer order: slowest-changing first → fastest-changing last
# ============================================================
FROM bellsoft/liberica-openjre-debian:25-cds AS builder
WORKDIR /builder
ARG JAR_FILE
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ============================================================
# Stage 2: AOT training run — generates app.aot cache
# Spring context initializes fully then exits cleanly.
# Training env vars are scoped to this stage only.
# ============================================================
FROM bellsoft/liberica-openjre-debian:25-cds AS aot-trainer
WORKDIR /application
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
ENV CAIMAN_SERVER_PORT=8080 \
    CAIMAN_SERVER_ENDPOINTS_PREFIX=/api \
    CAIMAN_SERVER_LOGGING_LEVEL=WARN \
    CAIMAN_SERVER_LOGGING_FOLDER_PATH=/tmp/logs \
    CAIMAN_SERVER_LOGGING_FORMAT=UNSTRUCTURED \
    CAIMAN_SERVER_DATABASE_TYPE=SQLITE \
    CAIMAN_SERVER_DATABASE_SQLITE_FILE=/tmp/training.db \
    CAIMAN_SERVER_OPEN_API_API_DOCS_PATH=/api-docs \
    CAIMAN_SERVER_OPEN_API_API_DOCS_ENABLE=false \
    CAIMAN_SERVER_OPEN_API_SWAGGER_UI_REDIRECT_PATH=/ \
    CAIMAN_SERVER_OPEN_API_SWAGGER_UI_ENABLE=false
RUN java -XX:AOTCacheOutput=app.aot \
         -Dspring.context.exit=onRefresh \
         org.springframework.boot.loader.launch.JarLauncher

# ============================================================
# Stage 3: Runtime — layers + AOT cache + tuned JVM flags
# No training env vars; all runtime config from docker run --env-file
# ============================================================
FROM bellsoft/liberica-openjre-debian:25-cds
WORKDIR /application
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
COPY --from=aot-trainer /application/app.aot ./
ENTRYPOINT ["java", \
  "-XX:AOTCache=app.aot", \
  "-XX:+UseContainerSupport", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseZGC", \
  "-Xss256k", \
  "org.springframework.boot.loader.launch.JarLauncher"]
```

**JVM flags rationale:**
- `AOTCache=app.aot` — load pre-trained class/JIT cache (50-75% startup reduction)
- `UseContainerSupport` — JVM respects cgroup memory limits (default Java 10+, explicit for clarity)
- `MaxRAMPercentage=75.0` — heap ceiling at 75% of container RAM; 25% left for non-heap (metaspace, I/O, threads)
- `InitialRAMPercentage=50.0` — avoids GC pressure from growing heap from a small initial size
- `UseZGC` — ultra-low pause GC, optimal with virtual threads (project has `spring.threads.virtual.enabled=true`)
- `Xss256k` — platform thread stack from 512k (JVM default) → 256k; safe because virtual threads use separate stack; reduces memory for carrier thread pool

---

### Task 3: Update Makefile

**Files:**
- Modify: `Makefile`

The current `build/jvm/docker` target (lines 86-94) calls `bootBuildImage`. Replace with `build/jar` + `build/docker-jvm`.

- [ ] **Step 1: Remove the build/jvm/docker block (lines 86-94)**

Remove these exact lines from `Makefile`:
```makefile
## build/jvm/docker: Build JVM Docker image using Buildpacks (CNB)
.PHONY: build/jvm/docker
build/jvm/docker:
	@START=$$(date +%s) && \
	echo 'Building JVM Docker image...' && \
	./gradlew :caiman-app:bootBuildImage -x test && \
	END=$$(date +%s) && \
	ELAPSED=$$((END-START)) && \
	echo "Docker JVM build completed in $$((ELAPSED/3600))h $$(((ELAPSED%3600)/60))m $$((ELAPSED%60))s"
```

- [ ] **Step 2: Add build/jar and build/docker-jvm after the build/jvm block**

After the `build/jvm` block (ending at line 84), insert — recipe lines **must use real tabs**:

```makefile
## build/jar: Build the bootJar (run before build/docker-jvm if code changed)
.PHONY: build/jar
build/jar:
	@START=$$(date +%s) && \
	echo 'Building bootJar...' && \
	./gradlew :caiman-app:bootJar -x test && \
	END=$$(date +%s) && \
	ELAPSED=$$((END-START)) && \
	echo "bootJar built in $$((ELAPSED/3600))h $$(((ELAPSED%3600)/60))m $$((ELAPSED%60))s"

## build/docker-jvm: Build JVM Docker image from Dockerfile.jvm (run build/jar first)
.PHONY: build/docker-jvm
build/docker-jvm:
	@START=$$(date +%s) && \
	JAR=caiman-app/build/libs/caiman-app-$(PROJECT_VERSION).jar && \
	echo "Building Docker JVM image from $$JAR..." && \
	docker build -f Dockerfile.jvm \
	  --build-arg JAR_FILE=$$JAR \
	  -t $(REGISTRY_HOST)/$(PROJECT_NAME):$(PROJECT_VERSION)-jvm . && \
	END=$$(date +%s) && \
	ELAPSED=$$((END-START)) && \
	echo "Docker JVM build completed in $$((ELAPSED/3600))h $$(((ELAPSED%3600)/60))m $$((ELAPSED%60))s"
```

- [ ] **Step 3: Verify make help shows correct targets**

```bash
make help
```

Expected under `BUILD > JVM`:
```
build/jvm          Build the project to be run on a JVM environment
build/jar          Build the bootJar (run before build/docker-jvm if code changed)
build/docker-jvm   Build JVM Docker image from Dockerfile.jvm (run build/jar first)
```

`build/jvm/docker` must NOT appear.

---

### Task 4: Build image and validate end-to-end

**Files:** None modified — validation only.

- [ ] **Step 1: Build the bootJar**

```bash
make build/jar
```

Expected: ends with `bootJar built in ...`. Confirm JAR exists:
```bash
ls -lh caiman-app/build/libs/*.jar
```

- [ ] **Step 2: Build the Docker image**

```bash
make build/docker-jvm
```

Expected: all 3 stages complete, ends with `Docker JVM build completed in ...`.

During Stage 2 (aot-trainer), expect Spring Boot startup logs and Liquibase running migrations on `/tmp/training.db` — this is normal. The stage succeeds when the app exits cleanly (exit code 0).

**If Stage 2 fails with missing required env var:** `CaimanServerPropsConfig` startup validation prints the missing var name. Add it to the `ENV` block in the `aot-trainer` stage with a safe training value, rebuild.

**If Stage 2 exits with non-zero:** Check Docker build output for the Java exception. Most likely cause: a required env var missing from the training `ENV` block.

- [ ] **Step 3: Confirm image in local Docker**

```bash
docker image ls under7/caiman-server
```

Expected: row with tag `<version>-jvm`.

- [ ] **Step 4: Prepare volumes (if not already done)**

```bash
mkdir -p /tmp/docker_volumes/caiman-server/logs/unstructured \
         /tmp/docker_volumes/caiman-server/logs/structured \
         /tmp/docker_volumes/caiman-server/db
chmod -R 777 /tmp/docker_volumes/caiman-server
```

- [ ] **Step 5: Run container**

```bash
VERSION=$(./gradlew properties -q --console=plain 2>/dev/null | grep '^version:' | awk '{print $2}')
docker run -d --name caiman-test \
  --env-file .env \
  -e CAIMAN_SERVER_LOGGING_FOLDER_PATH=/app/logs \
  -e CAIMAN_SERVER_DATABASE_SQLITE_FILE=/app/data/caiman.db \
  -p 8080:8080 \
  -v /tmp/docker_volumes/caiman-server/logs:/app/logs \
  -v /tmp/docker_volumes/caiman-server/db:/app/data \
  under7/caiman-server:${VERSION}-jvm
```

- [ ] **Step 6: Check startup time — primary AOT validation**

```bash
sleep 5 && docker logs caiman-test 2>&1 | grep -E "Started|seconds"
```

Expected: `Started CaimanServerApplication in X.XXX seconds`. With AOT cache active, expect noticeably faster than the previous Buildpack result (3.736s). If startup is similar or slower, check that `app.aot` was copied correctly: `docker exec caiman-test ls -lh /application/app.aot`.

- [ ] **Step 7: Run curl validation — must return HTTP 201**

```bash
HTTP_STATUS=$(curl -s -o /tmp/caiman_response.json -w "%{http_code}" \
  --request POST \
  --url http://localhost:8080/api/v1/debtors \
  --header 'content-type: application/json' \
  --header 'x-channel: Bruno' \
  --header 'x-correlation-id: d76e7f4b-87e9-4858-8bcb-a86a2db52c75' \
  --data '{
  "name": "Gggwp",
  "notes": "eu mesmo",
  "notificationsEnabled": true,
  "active": true,
  "contacts": [
    {
      "contactType": "EMAIL",
      "contactValue": "admin@gmail.com",
      "priority": 1
    }
  ]
}')
echo "HTTP Status: $HTTP_STATUS"
cat /tmp/caiman_response.json
```

Expected: `HTTP Status: 201`, body contains `"name":"Gggwp"`.

If not 201: check `docker logs caiman-test` for root cause. Fix, stop container (`docker stop caiman-test && docker rm caiman-test`), restart from Step 5.

- [ ] **Step 8: Collect metrics for comparison with Buildpack baseline**

```bash
docker image ls under7/caiman-server --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
docker stats caiman-test --no-stream --format "table {{.Container}}\t{{.MemUsage}}\t{{.MemPerc}}"
```

Buildpack baseline: 797 MB image, 553 MiB memory, 3.736s startup.

- [ ] **Step 9: Stop and remove test container**

```bash
docker stop caiman-test && docker rm caiman-test
```

- [ ] **Step 10: Report results**

Report:
- Image size (vs 797 MB Buildpack)
- Startup time (vs 3.736s Buildpack) — key metric for AOT validation
- Memory usage (vs 553 MiB Buildpack)
