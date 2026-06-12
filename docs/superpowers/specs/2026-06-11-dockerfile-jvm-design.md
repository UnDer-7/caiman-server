# Dockerfile JVM — Design Spec

**Date:** 2026-06-11
**Scope:** Replace Buildpack config with `Dockerfile.jvm` — layered JAR, AOT Cache, JVM flags, two Makefile targets

---

## Goal

Replace `bootBuildImage` (Buildpack) with a hand-authored `Dockerfile.jvm` using Spring Boot 4 best practices: layered JAR extraction, Java 25 AOT Cache training baked into the image, and explicit JVM memory flags for container awareness.

---

## Architecture

Three-stage multi-stage build:

```
Stage 1 (builder)      — extract bootJar into 4 Docker layers via jarmode=tools
Stage 2 (aot-trainer)  — training run: app starts/stops, writes app.aot
Stage 3 (runtime)      — copy layers + app.aot, ENTRYPOINT with JVM flags
```

Base image: `bellsoft/liberica-openjre-debian:25-cds` (JRE only, CDS-ready, matches project Java 25).

---

## Files

| Action | Path |
|--------|------|
| Create | `Dockerfile.jvm` (project root) |
| Modify | `caiman-app/build.gradle.kts` — remove `BootBuildImage` import + task block |
| Modify | `Makefile` — replace `build/docker-jvm`, add `build/jar` |

---

## Dockerfile.jvm

### Stage 1: builder

Extracts bootJar into 4 layers (slow-changing first → fast-changing last for Docker cache):

```dockerfile
FROM bellsoft/liberica-openjre-debian:25-cds AS builder
WORKDIR /builder
ARG JAR_FILE
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted
```

Layers produced: `dependencies/`, `spring-boot-loader/`, `snapshot-dependencies/`, `application/`

### Stage 2: aot-trainer

Runs the app with `-Dspring.context.exit=onRefresh` — Spring initializes the full context then exits cleanly. JVM writes class load + JIT metadata to `app.aot`.

Training env vars are temporary values used only during build; they do not appear in the runtime stage and do not leak to production.

```dockerfile
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
         -jar application.jar
```

### Stage 3: runtime

Copies layers and `app.aot`. No training env vars carried forward — runtime config comes entirely from `docker run --env-file`.

```dockerfile
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
  "-jar", "application.jar"]
```

**JVM flags rationale:**
- `AOTCache=app.aot` — load pre-trained class/JIT cache → 50-75% startup reduction
- `UseContainerSupport` — JVM reads cgroup memory limits (default Java 10+, explicit for clarity)
- `MaxRAMPercentage=75.0` — heap ceiling at 75% of container RAM; leaves 25% for non-heap (threads, I/O, metaspace)
- `InitialRAMPercentage=50.0` — avoid GC pressure from growing heap from small initial size
- `UseZGC` — low-pause GC, works well with virtual threads (project uses `spring.threads.virtual.enabled=true`)
- `Xss256k` — platform thread stack size; 256k vs JVM default 512k; safe with virtual threads (carrier thread count is low)

---

## Makefile Changes

Remove current `build/docker-jvm` (was bootBuildImage). Add two targets under `## ----- JVM -----`:

```makefile
## build/jar: Build the bootJar (run before build/docker-jvm if code changed)
.PHONY: build/jar
build/jar:
	@START=$$(date +%s) && \
	echo 'Building bootJar...' && \
	./gradlew :caiman-app:bootJar -x test && \
	END=$$(date +%s) && ELAPSED=$$((END-START)) && \
	echo "bootJar built in $$((ELAPSED/3600))h $$(((ELAPSED%3600)/60))m $$((ELAPSED%60))s"

## build/docker-jvm: Build JVM Docker image from Dockerfile.jvm (run build/jar first)
.PHONY: build/docker-jvm
build/docker-jvm:
	@START=$$(date +%s) && \
	JAR=$$(find caiman-app/build/libs -name "*.jar" ! -name "*-plain.jar" | head -1) && \
	echo "Building Docker JVM image from $$JAR..." && \
	docker build -f Dockerfile.jvm \
	  --build-arg JAR_FILE=$$JAR \
	  -t $(REGISTRY_HOST)/$(PROJECT_NAME):$(PROJECT_VERSION)-jvm . && \
	END=$$(date +%s) && ELAPSED=$$((END-START)) && \
	echo "Docker JVM build completed in $$((ELAPSED/3600))h $$(((ELAPSED%3600)/60))m $$((ELAPSED%60))s"
```

`build/jvm` (the existing plain Gradle target) is kept as-is — still useful for non-Docker JVM deployments.

---

## `caiman-app/build.gradle.kts` — Removals

Remove:
```kotlin
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
```
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

---

## docker run command (unchanged from previous validated setup)

```bash
# Prepare volumes (first time only)
mkdir -p /tmp/docker_volumes/caiman-server/logs/unstructured \
         /tmp/docker_volumes/caiman-server/logs/structured \
         /tmp/docker_volumes/caiman-server/db
chmod -R 777 /tmp/docker_volumes/caiman-server

VERSION=$(./gradlew properties -q --console=plain 2>/dev/null | grep '^version:' | awk '{print $2}')
docker run --rm \
  --env-file .env \
  -e CAIMAN_SERVER_LOGGING_FOLDER_PATH=/app/logs \
  -e CAIMAN_SERVER_DATABASE_SQLITE_FILE=/app/data/caiman.db \
  -p 8080:8080 \
  -v /tmp/docker_volumes/caiman-server/logs:/app/logs \
  -v /tmp/docker_volumes/caiman-server/db:/app/data \
  under7/caiman-server:${VERSION}-jvm
```

---

## Validation Steps

1. `make build/jar` → bootJar produzido em `caiman-app/build/libs/`
2. `make build/docker-jvm` → imagem `under7/caiman-server:X.X.X-jvm` criada
3. Container sobe e startup é perceptivelmente menor que 3.7s (AOT funcionando)
4. `curl POST /api/v1/debtors` → HTTP 201
5. Reportar: tamanho da imagem, memória, tempo de startup
