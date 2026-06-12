# Docker Buildpack JVM — Design Spec

**Date:** 2026-06-11  
**Scope:** Configure `bootBuildImage` (CNB/Paketo) for JVM-only local Docker build + Makefile target

---

## Goal

Build an optimized JVM Docker image via Cloud Native Buildpacks (Paketo) using the Spring Boot Gradle plugin. No image publish. Local Docker daemon only.

---

## Architecture

Spring Boot 4.0.6 Gradle plugin exposes `bootBuildImage` task on the `:caiman-app` module. Configuration lives in `caiman-app/build.gradle.kts`. Makefile provides a developer-facing target that delegates to this task.

---

## Configuration — `caiman-app/build.gradle.kts`

Add `bootBuildImage` block:

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

**Decisions:**
- `imageName` suffix `-jvm` differentiates from future `-native` (GraalVM) variant
- Builder: default `paketobuildpacks/builder-noble-java-tiny:latest` — already the smallest; not overridden
- `BP_JVM_TYPE=JRE` removes JDK tools/compiler from the runtime image → smaller size
- `BP_JVM_VERSION=25` matches project Java version (AGENT.md)
- `BPL_JVM_THREAD_COUNT=50` reduces default thread stack reservation (project uses virtual threads)
- `publish=false` enforces local-only build

---

## Makefile — `BUILD > JVM` sub-block

Append target after existing `build/jvm`:

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

---

## Volumes

Both paths must be prepared on the host before running the container.

| Volume | Host path | Container path | Required |
|--------|-----------|----------------|----------|
| Logs | `/tmp/docker_volumes/caiman-server/logs` | `/app/logs` | Yes |
| SQLite | `/tmp/docker_volumes/caiman-server/db` | `/app/data` | No |

Container-side paths set via env vars:
- `CAIMAN_SERVER_LOGGING_FOLDER_PATH=/app/logs`
- `CAIMAN_SERVER_DATABASE_SQLITE_FILE=/app/data/caiman.db`

**Host preparation required (Paketo runs as non-root user `cnb`, uid 1000):**

```bash
# Logs: logback creates unstructured/ and structured/ subdirs — pre-create them
mkdir -p /tmp/docker_volumes/caiman-server/logs/unstructured
mkdir -p /tmp/docker_volumes/caiman-server/logs/structured
chmod -R 777 /tmp/docker_volumes/caiman-server/logs

# SQLite: mount the directory (not the file) — SQLite needs WAL/journal siblings
mkdir -p /tmp/docker_volumes/caiman-server/db
chmod -R 777 /tmp/docker_volumes/caiman-server/db
```

---

## Validated `docker run` command

```bash
docker run --rm \
  --env-file .env \
  -e CAIMAN_SERVER_LOGGING_FOLDER_PATH=/app/logs \
  -e CAIMAN_SERVER_DATABASE_SQLITE_FILE=/app/data/caiman.db \
  -p 8080:8080 \
  -v /tmp/docker_volumes/caiman-server/logs:/app/logs \
  -v /tmp/docker_volumes/caiman-server/db:/app/data \
  under7/caiman-server:${PROJECT_VERSION}-jvm
```

The `-e` flags override the dev-local values in `.env` with container-internal absolute paths. `.env` is not modified.

---

## Validation Results (completed 2026-06-11)

1. `make build/docker-jvm` — ✅ built in 32s (Java 25 / BellSoft Liberica JRE 25.0.3)
2. Container started in 3.736s, Liquibase applied 13 changesets
3. `curl POST /api/v1/debtors` — ✅ HTTP 201 Created (correct REST semantics for resource creation)
4. Image size: **797 MB** (content 389 MB) · Memory: **553.5 MiB**
