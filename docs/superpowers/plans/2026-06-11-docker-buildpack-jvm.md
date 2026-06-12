# Docker Buildpack JVM Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Configure `bootBuildImage` in `caiman-app` for optimized JVM Docker image via CNB/Paketo, add Makefile target, validate end-to-end with volumes and curl.

**Architecture:** Spring Boot 4 Gradle plugin `bootBuildImage` task configured in `caiman-app/build.gradle.kts` using Paketo `paketobuildpacks/builder-noble-java-tiny` (already the default — not overridden). Makefile delegates to `./gradlew :caiman-app:bootBuildImage -x test`. Container-side paths for logs and SQLite are set via `-e` overrides at `docker run` time, leaving `.env` unchanged for dev use.

**Tech Stack:** Spring Boot 4.0.6 Gradle Plugin (`bootBuildImage`), Paketo Buildpacks (`paketobuildpacks/builder-noble-java-tiny`), Docker, Makefile, Kotlin DSL

---

### Task 1: Configure `bootBuildImage` in `caiman-app/build.gradle.kts`

**Files:**
- Modify: `caiman-app/build.gradle.kts`

- [ ] **Step 1: Add `BootBuildImage` import**

In `caiman-app/build.gradle.kts`, the first line is already:
```kotlin
import org.apache.tools.ant.filters.ReplaceTokens
```

Add the `BootBuildImage` import directly after it:

```kotlin
import org.apache.tools.ant.filters.ReplaceTokens
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
```

- [ ] **Step 2: Append `bootBuildImage` task block at end of file**

After the `dependencies { }` block, append:

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

**Why each env var:**
- `BP_JVM_VERSION=25` — matches project Java version (AGENT.md); builder defaults to 21 otherwise
- `BP_JVM_TYPE=JRE` — installs JRE instead of JDK in image → removes compiler/tools → smaller image
- `BPL_JVM_THREAD_COUNT=50` — Paketo memory calculator uses thread count to reserve stack memory; project uses virtual threads so platform thread count is low; reduces reserved memory

- [ ] **Step 3: Verify Gradle recognizes the task**

```bash
./gradlew :caiman-app:tasks --group build 2>/dev/null | grep -i image
```

Expected output:
```
bootBuildImage - Builds an OCI image of the application using the output of the bootJar task
```

---

### Task 2: Add `build/docker-jvm` target to Makefile

**Files:**
- Modify: `Makefile`

- [ ] **Step 1: Add target after the `build/jvm` block**

In `Makefile`, the `build/jvm` block ends at line 84 (the closing `echo` line). Append the following **immediately after** that block. Indentation MUST be a real tab character, not spaces:

```makefile
## build/docker-jvm: Build JVM Docker image using Buildpacks (CNB)
.PHONY: build/docker-jvm
build/docker-jvm:
	@START=$$(date +%s) && \
	echo 'Building JVM Docker image...' && \
	./gradlew :caiman-app:bootBuildImage -x test && \
	END=$$(date +%s) && ELAPSED=$$((END-START)) && \
	echo "Docker JVM build completed in $$((ELAPSED/3600))h $$(((ELAPSED%3600)/60))m $$((ELAPSED%60))s"
```

- [ ] **Step 2: Verify `make help` shows the new target**

```bash
make help
```

Expected: `build/docker-jvm` listed under the `BUILD > JVM` section with description `Build JVM Docker image using Buildpacks (CNB)`.

---

### Task 3: Build image and validate end-to-end

**Files:** None modified — validation only.

- [ ] **Step 1: Build the image**

```bash
make build/docker-jvm
```

Expected: ends with `Docker JVM build completed in ...`. On first run, Paketo pulls `paketobuildpacks/builder-noble-java-tiny` — this takes a few minutes. Subsequent builds use the cached builder.

If it fails with `BP_JVM_VERSION` not found: Paketo may not yet ship Java 25 in the tiny builder. Fallback: change `BP_JVM_VERSION` to `"21"` in the task and rebuild.

- [ ] **Step 2: Confirm image exists in local Docker**

```bash
docker image ls under7/caiman-server
```

Expected:
```
REPOSITORY               TAG             IMAGE ID       CREATED        SIZE
under7/caiman-server     X.X.X-jvm       <id>           ...            <size>
```

- [ ] **Step 3: Create host volume paths**

```bash
mkdir -p /tmp/docker_volumes/caiman-server/logs
mkdir -p /tmp/docker_volumes/caiman-server/db
touch /tmp/docker_volumes/caiman-server/db/caiman.db
```

- [ ] **Step 4: Resolve the image version**

```bash
VERSION=$(./gradlew properties -q --console=plain 2>/dev/null | grep '^version:' | awk '{print $2}')
echo "Image tag: under7/caiman-server:${VERSION}-jvm"
```

- [ ] **Step 5: Run container with volumes**

```bash
docker run -d --name caiman-test \
  --env-file .env \
  -e CAIMAN_SERVER_LOGGING_FOLDER_PATH=/app/logs \
  -e CAIMAN_SERVER_DATABASE_SQLITE_FILE=/app/data/caiman-server.db \
  -p 8080:8080 \
  -v /tmp/docker_volumes/caiman-server/logs:/app/logs \
  -v /tmp/docker_volumes/caiman-server/db/caiman.db:/app/data/caiman-server.db \
  under7/caiman-server:${VERSION}-jvm
```

**Why `-e` overrides:** `.env` has dev-local paths (`/tmp/caiman-server-logs`, `./sqlite_database/database.db`). The `-e` flags replace them with absolute container-internal paths that match the volume mounts. `.env` is not modified.

- [ ] **Step 6: Wait for startup and confirm healthy**

```bash
sleep 10 && docker logs caiman-test 2>&1 | tail -20
```

Expected: Spring Boot startup log ending with `Started CaimanServerApplication in X.XXX seconds`. No stack traces or fatal errors.

If it crashes (exit code non-zero): check full logs with `docker logs caiman-test`. Common causes: missing required env var (startup validation in `CaimanServerPropsConfig` rejects app immediately), SQLite file not writable (check host file permissions).

- [ ] **Step 7: Run curl validation — must return HTTP 200**

```bash
curl -s -o /tmp/caiman_response.json -w "%{http_code}" \
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
}'
```

Then check:
```bash
echo "" && cat /tmp/caiman_response.json | python3 -m json.tool
```

Expected: HTTP status `200`, response body is a debtor JSON object with `id`, `name: "Gggwp"`.

If not 200: check `docker logs caiman-test` for the error. Fix the root cause, stop the container (`docker stop caiman-test && docker rm caiman-test`), and repeat from Step 5.

- [ ] **Step 8: Collect image size**

```bash
docker image ls under7/caiman-server --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
```

Record the size for the final report.

- [ ] **Step 9: Collect container memory usage**

```bash
docker stats caiman-test --no-stream --format "table {{.Container}}\t{{.MemUsage}}\t{{.MemPerc}}"
```

Record the memory for the final report.

- [ ] **Step 10: Stop and remove test container**

```bash
docker stop caiman-test && docker rm caiman-test
```

- [ ] **Step 11: Report results**

Report:
- What was configured and how Buildpacks work (brief)
- Image size (from Step 8)
- Memory usage (from Step 9)
