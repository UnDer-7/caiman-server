# Changelog

All notable changes to caiman-server will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/) and this project adheres to [Semantic Versioning](https://semver.org/).

## [v0.0.2] - 2026-06-22

### Changed

#### CI/CD — Script Extraction

- Extracted all large inline bash blocks from workflow YAML into standalone scripts under `.github/scripts/`
  - `validate-version.sh` — SemVer comparison + tag existence check (replaces duplicated logic in both pipelines)
  - `validate-changelog.sh` — asserts CHANGELOG.md has a documented entry with `####` sections for the current version
  - `find-build-artifacts.sh` — locates JAR and native binary after build; writes paths to `$GITHUB_OUTPUT`
  - `verify-downloaded-artifacts.sh` — verifies downloaded artifacts exist and marks native binary executable
  - `extract-changelog.sh` — extracts the relevant CHANGELOG section and appends Docker/asset tables for release notes
  - `build-security-alert-email.sh` — renders HTML + plaintext alert email for security scan failures
  - `create-or-update-security-issue.sh` — creates or appends a comment to an existing GitHub issue on scan failure

#### CI/CD — Workflow Improvements

- Added `defaults: run: shell: bash` to all three workflows (`deploy.yml`, `pull-request-pipeline.yml`, `weekly-security-scan.yml`)
- `deploy.yml`: added explicit version validation step before tests run; fixed unquoted variable expansion in version output steps
- `pull-request-pipeline.yml`: replaced inline version comparison and changelog validation with script calls
- `weekly-security-scan.yml`: deduplicated email/issue generation by delegating to shared scripts; fixed issue title separator (`|` → `::`)

#### Build

- `justfile`: `project-version` now reads directly from `build.gradle.kts` via `grep` (no Gradle invocation needed); `version-set` now prints old → new version on change

## [v0.0.1] - 2026-06-16

### Added

#### Debtor Management
- `POST /api/debtors` — register a new debtor with name, notes, and contact list

#### Infrastructure
- `Dockerfile.jvm` — multi-stage JVM image with CDS AOT training run; based on Liberica OpenJRE 25 Alpine
- `Dockerfile.native` — GraalVM native image on Debian bookworm-slim; no JVM required at runtime
- Docker Compose setup for local development
- SQLite support as zero-config local database
- PostgreSQL support for production via HikariCP connection pool
- Liquibase-managed schema migrations (YAML changelogs)
- GraalVM native compilation support with AOT reflection hints per bounded context
