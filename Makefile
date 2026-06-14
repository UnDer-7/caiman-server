# ==================================================================================== #
# VARIABLES
# ==================================================================================== #
PROJECT_NAME=caiman-server
REGISTRY_HOST=under7
PROJECT_VERSION := $(shell ./gradlew :caiman-app:properties -q --console=plain 2>/dev/null | grep '^version:' | awk '{print $$2}')




# ==================================================================================== #
## ===== HELPERS =====
# ==================================================================================== #
## help: Describe all available targets
.PHONY: help
help:
	@echo 'Usage: make <target>'
	@sed -n 's/^##//p' $(MAKEFILE_LIST) | column -t -s ':' | \
	awk 'BEGIN {first=1} \
						/^ *=====/ { if (!first) print ""; print "   " $$0; first=0; next } \
						/^ *-----/ { print ""; print "   " $$0; next } \
						{ print "   " $$0 }'

# Hidden
.PHONY: all
all: help




# ==================================================================================== #
## ===== DEV =====
# ==================================================================================== #
## dev/clean: Clean all build outputs
.PHONY: dev/clean
dev/clean:
	./gradlew clean --rerun-tasks

## dev/compile: Just compile the application
.PHONY: dev/compile
dev/compile:
	@echo ">>> Compiling…"
	./gradlew clean compileJava --rerun-tasks

## dev/run: Build all modules and run the application locally (loads .env variables)
.PHONY: dev/run
dev/run:
	@echo ">>> Loading .env and starting application…"
	@set -a && \
	eval $$(grep -v '^\s*#' .env | grep -v '^\s*$$' | sed 's/\r$$//') && \
	set +a && \
	./gradlew :caiman-app:bootRun

## ----- Tests -----
## test/unit: Run unit tests across all modules
.PHONY: test/unit
test/unit:
	./gradlew clean unitTest --rerun-tasks

## test/integration/jvm: Run integration tests on JVM across all modules
.PHONY: test/integration/jvm
test/integration/jvm:
	./gradlew clean integrationTestJvm --rerun-tasks

## test/integration/native: Run integration tests in GraalVM native mode (requires Docker)
.PHONY: test/integration/native
test/integration/native:
	./gradlew :caiman-app:integrationTestNative --rerun-tasks

## test: Run unit and integration tests (JVM) across all modules
.PHONY: test
test:
	./gradlew clean unitTest integrationTestJvm --rerun-tasks

## test/coverage: Run all tests (JVM) and generate aggregated JaCoCo report
.PHONY: test/coverage
test/coverage:
	./gradlew clean unitTest integrationTestJvm jacocoRootReport --rerun-tasks



# ==================================================================================== #
## ===== BUILD =====
# ==================================================================================== #
## build/artifacts: Build GraalVM native image + bootJar in a single Gradle invocation (loads .env variables)
.PHONY: build/artifacts
build/artifacts:
	@START=$$(date +%s) && \
	echo 'Loading .env and building native image + bootJar...' && \
	set -a && \
	eval $$(grep -v '^\s*#' .env | grep -v '^\s*$$' | sed 's/\r$$//') && \
	set +a && \
	./gradlew :caiman-app:nativeCompile :caiman-app:bootJar -x test --rerun-tasks && \
	END=$$(date +%s) && \
	ELAPSED=$$((END-START)) && \
	echo "Artifacts built in $$((ELAPSED/3600))h $$(((ELAPSED%3600)/60))m $$((ELAPSED%60))s"

## ----- JVM -----
## build/jar: Build the bootJar (loads .env variables, run before build/docker-jvm if code changed)
.PHONY: build/jar
build/jar:
	@START=$$(date +%s) && \
	echo 'Loading .env and building bootJar...' && \
	set -a && \
	eval $$(grep -v '^\s*#' .env | grep -v '^\s*$$' | sed 's/\r$$//') && \
	set +a && \
	./gradlew :caiman-app:bootJar -x test --rerun-tasks && \
	END=$$(date +%s) && \
	ELAPSED=$$((END-START)) && \
	echo "bootJar built in $$((ELAPSED/3600))h $$(((ELAPSED%3600)/60))m $$((ELAPSED%60))s"

## build/jar/docker: Build JVM Docker image from Dockerfile.jvm (run build/jar first)
.PHONY: build/jar/docker
build/jar/docker:
	@START=$$(date +%s) && \
	echo "Building Docker JVM image..." && \
	docker build -f Dockerfile.jvm \
	  -t $(REGISTRY_HOST)/$(PROJECT_NAME):$(PROJECT_VERSION)-jvm . && \
	END=$$(date +%s) && \
	ELAPSED=$$((END-START)) && \
	echo "Docker JVM build completed in $$((ELAPSED/3600))h $$(((ELAPSED%3600)/60))m $$((ELAPSED%60))s"

## ----- GRAALVM -----

## build/native: Build GraalVM native image (loads .env variables)
.PHONY: build/native
build/native:
	@START=$$(date +%s) && \
	echo 'Loading .env and building native image...' && \
	set -a && \
	eval $$(grep -v '^\s*#' .env | grep -v '^\s*$$' | sed 's/\r$$//') && \
	set +a && \
	./gradlew :caiman-app:nativeCompile -x test --rerun-tasks && \
	END=$$(date +%s) && \
	ELAPSED=$$((END-START)) && \
	echo "Native build completed in $$((ELAPSED/3600))h $$(((ELAPSED%3600)/60))m $$((ELAPSED%60))s"

## build/native/docker: Build native Docker image from Dockerfile.native (run build/native first)
.PHONY: build/native/docker
build/native/docker:
	@START=$$(date +%s) && \
	echo "Building Docker native image..." && \
	docker build -f Dockerfile.native \
	  -t $(REGISTRY_HOST)/$(PROJECT_NAME):$(PROJECT_VERSION)-native . && \
	END=$$(date +%s) && \
	ELAPSED=$$((END-START)) && \
	echo "Docker native build completed in $$((ELAPSED/3600))h $$(((ELAPSED%3600)/60))m $$((ELAPSED%60))s"


# ==================================================================================== #
## ===== VERSION =====
# ==================================================================================== #
## version: Show current project version
.PHONY: version
version:
	@grep -oP 'version = "\K[^"]+' build.gradle.kts | head -1

## version/set: Set project version  (usage: make version/set 0.1.2)
.PHONY: version/set
version/set:
	@if [ -z "$(filter-out version/set,$(MAKECMDGOALS))" ]; then \
		echo "ERROR: Version number is required"; \
		echo "Usage: make version/set 0.1.2"; \
		exit 1; \
	fi
	@sed -i 's/version = "[^"]*"/version = "$(filter-out version/set,$(MAKECMDGOALS))"/' build.gradle.kts
	@echo "Version → $(filter-out version/set,$(MAKECMDGOALS))"

# Prevent make from treating version number as a target
%:
	@:
