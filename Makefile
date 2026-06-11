# ==================================================================================== #
# VARIABLES
# ==================================================================================== #
PROJECT_NAME=caiman-server
REGISTRY_HOST=under7
PROJECT_VERSION := $(shell ./gradlew properties -q --console=plain 2>/dev/null | grep '^version:' | awk '{print $$2}')




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

## test/integration: Run integration tests across all modules
.PHONY: test/integration
test/integration:
	./gradlew clean integrationTest --rerun-tasks

## test: Run unit and integration tests across all modules
.PHONY: test
test:
	./gradlew clean unitTest integrationTest --rerun-tasks

## test/coverage: Run all tests and generate aggregated JaCoCo report
.PHONY: test/coverage
test/coverage:
	./gradlew clean unitTest integrationTest jacocoRootReport --rerun-tasks



# ==================================================================================== #
## ===== BUILD =====
# ==================================================================================== #
## ----- JVM -----
## build/jvm: Build the project to be run on a JVM environment
.PHONY: build/jvm
build/jvm:
	@START=$$(date +%s) && \
	echo 'Building for JVM...' && \
	./gradlew :caiman-app:bootJar -x test && \
	END=$$(date +%s) && \
	ELAPSED=$$((END-START)) && \
	echo "JVM build completed in $$((ELAPSED/3600))h $$(((ELAPSED%3600)/60))m $$((ELAPSED%60))s"
