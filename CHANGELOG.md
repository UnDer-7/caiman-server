# Changelog

All notable changes to caiman-server will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/) and this project adheres to [Semantic Versioning](https://semver.org/).

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
