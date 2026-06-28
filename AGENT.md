# Caiman — Agent Instructions

## CRITICAL: Language

**All code, documentation, comments, commit messages, variable names, class names, test descriptions, and any other written artifact in this project must be in English.** No exceptions — Portuguese, Spanish, or any other language must not appear in the codebase.

---

## CRITICAL: Spring Boot 4

This project uses **Spring Boot 4**. Not Spring Boot 3. Most search results, Stack Overflow answers, and LLM training data reference Spring Boot 3 — that information is often **not applicable here**.

Key differences to watch:
- **Jackson imports changed significantly.** Many packages moved. Do not assume Spring Boot 3 import paths.
- API deprecations and removals differ from Spring Boot 3.
- When in doubt about any Spring/Spring Boot API, consult the official migration guide before writing code:
  **https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide**

---

## What is this project

Caiman is an open-source, self-hosted personal billing management system. It allows a single admin to track and collect informal recurring or one-time shared expenses from a small group of people.

**This repository is the backend API server only.** It exposes REST endpoints consumed by external clients (web app, Telegram bot, AI agent, etc.). Each client lives in its own repository. This project has no frontend, no HTML, no template rendering.

Core use cases:

- **Rotating shared subscription**: one person pays per cycle in a defined order (e.g. a shared YouTube Premium plan where each member pays one month per turn).  
- **Split expense**: all members are charged each cycle, dividing a fixed total (e.g. an AirBnB trip split among 5 people).

Payment is confirmed by the debtor uploading a receipt (PIX, bank transfer, Venmo, etc.). An AI agent validates the receipt. Debtors never log in — they interact only via tokenized links received by email.

## Stack

- **Backend**: Java 25, Spring Boot 4, Spring Data JPA, Hibernate, Spring Mail  
- **Database**: PostgreSQL (primary), SQLite (fallback) — selected via `CAIMAN_SERVER_DATABASE_TYPE`  
- **Migrations**: Liquibase (YAML changelogs)  
- **AI**: Anthropic API (claude-sonnet model) for payment proof analysis  
- **Build**: Gradle (multi-project, Kotlin DSL)  
- **Deployment**: Docker Compose

---

## Environment Variables

**`.env` is the single source of truth for all environment variables.** It is always committed to the repository.

### Rules

- Every env var the project reads must be declared in `.env`.
- All values in `.env` are local dev defaults — no secrets, no production credentials. Committing it is safe and intentional.
- If you need to add a new env var, add it to `.env`. Do not leave it undocumented or rely on implicit system vars.

### Secrets

`.env` must never contain actual secret values (API keys, tokens, passwords for real services). For secrets, use indirection:

```dotenv
# .env — committed, safe
CAIMAN_ANTHROPIC_API_KEY="${LOCAL_CAIMAN_ANTHROPIC_API_KEY}"
```

The project reads `CAIMAN_ANTHROPIC_API_KEY`. That variable references `LOCAL_CAIMAN_ANTHROPIC_API_KEY`, which the developer must set on their own machine (e.g. in `~/.zshrc` or `~/.bashrc`). The actual key never touches the repo.

**Naming convention for the machine-level variable:** prefix the project var name with `LOCAL_`.

---

## Database Configuration

The application supports two database backends selected at startup via a single env var. No container required for SQLite — preferred for local development.

### Switching databases

Set `CAIMAN_SERVER_DATABASE_TYPE` to one of:

| Value | Backend | When to use |
|---|---|---|
| `SQLITE` | SQLite (file-based) | Local dev, tests — no container needed |
| `POSTGRES` | PostgreSQL via HikariCP | Production, staging |

### Required env vars per type

**SQLite** — only one var required:
```
CAIMAN_SERVER_DATABASE_TYPE=SQLITE
CAIMAN_SERVER_DATABASE_SQLITE_FILE=./sqlite_database/database.db
```

**PostgreSQL** — three vars required:
```
CAIMAN_SERVER_DATABASE_TYPE=POSTGRES
CAIMAN_SERVER_DATABASE_JDBC_URL=jdbc:postgresql://localhost:5432/caiman_server
CAIMAN_SERVER_DATABASE_USERNAME=admin_usr
CAIMAN_SERVER_DATABASE_PASSWORD=admin_pass
```

Startup validation (`CaimanServerPropsConfig.DatabasePropImpl`) rejects the app immediately if the required vars for the selected type are missing.

### How it works internally

`DataSourceConfig` (`caiman-app`) reads `CaimanServerPropsConfig.database().type()` and builds the appropriate `DataSource` bean:

- **POSTGRES**: `HikariDataSource` with the provided JDBC URL, username, and password.
- **SQLITE**: `SQLiteDataSource` with a server-optimized config — WAL journal mode, 5 s busy timeout, NORMAL sync, MEMORY temp store, ~200 MB page cache, IMMEDIATE transaction mode, and foreign key enforcement. File path from `CAIMAN_SERVER_DATABASE_SQLITE_FILE`.

`JpaVendorAdapter` switches Hibernate dialect in the same switch: `PostgreSQLDialect` vs `SQLiteDialect` (Hibernate community dialect).

### Liquibase compatibility — `dbms: "!sqlite"` guard

SQLite does not support `ALTER TABLE ADD CONSTRAINT` (unique constraints must be declared at table creation). Any changeset that uses `addUniqueConstraint` **must** be guarded so it is skipped on SQLite:

```yaml
- changeSet:
    id: some-unique-constraint
    author: caiman
    dbms: "!sqlite"          # ← skip on SQLite
    preConditions:
      onFail: MARK_RAN
      not:
        uniqueConstraintExists:
          tableName: some_table
          constraintName: uq_some_constraint
    changes:
      - addUniqueConstraint:
          tableName: some_table
          columnNames: col_a, col_b
          constraintName: uq_some_constraint
```

The table creation changeset (without `dbms` guard) already declares the columns. The `addUniqueConstraint` changeset is only meaningful for PostgreSQL. Skipping it on SQLite is safe — enforce uniqueness via application-layer validation or a `UNIQUE` index on the `createTable` changeset instead if needed.

---

## Project structure

```
caiman-server/                        ← Gradle root project
  caiman-shared/                      ← Physical parent directory (not a Gradle module)
    contracts/                        ← [caiman-contracts] Domain events, gateways, common types, base exceptions
    web-support/                      ← [caiman-web-support] @CaimanEndpoint, WebMvcConfigurer, global exception handler
  caiman-debtor/                      ← Bounded context: Debtor management
    core/                             ← Domain model, use cases, port interfaces (spring-context + spring-tx only)
    entrypoint/                       ← Adapters/in: REST controllers
    infrastructure/                   ← Adapters/out: JPA repositories
  caiman-billing/                     ← Bounded context: ChargePlan, Member, Invoice, Odin scheduler
    core/                             ← Domain model, use cases, port interfaces (spring-context + spring-tx only)
    entrypoint/                       ← Adapters/in: REST controllers, OdinJob (@Scheduled)
    infrastructure/                   ← Adapters/out: JPA repositories, event publisher
  caiman-payment/                     ← Bounded context: PaymentProof, Payment, AI analysis
    core/                             ← Domain model, use cases, port interfaces (spring-context + spring-tx only)
    entrypoint/                       ← Adapters/in: REST controllers (public proof upload)
    infrastructure/                   ← Adapters/out: JPA repositories, Anthropic API client
  caiman-notification/                ← Bounded context: NotificationOutbox, NotificationLog, Huginn
    core/                             ← Domain model, use cases, port interfaces (spring-context + spring-tx only)
    entrypoint/                       ← Adapters/in: HuginnJob (@Scheduled), domain event listeners
    infrastructure/                   ← Adapters/out: JPA repositories, SMTP email sender
  caiman-app/                         ← Spring Boot main class, security config, composition root
    src/main/resources/db/            ← Liquibase changelog files
  docs/                               ← Project documentation (always consult before implementing)
```

### Module dependency rules

**No cross-module Maven/Gradle dependencies between bounded contexts.** All cross-context communication goes through `caiman-contracts`.

```
caiman-contracts
  └── no dependencies on other modules

caiman-web-support         → caiman-contracts, spring-web

caiman-debtor:core         → caiman-contracts
caiman-debtor:entrypoint   → caiman-debtor:core, caiman-web-support
caiman-debtor:infrastructure → caiman-debtor:core, caiman-contracts

caiman-billing:core        → caiman-contracts
caiman-billing:entrypoint  → caiman-billing:core, caiman-web-support
caiman-billing:infrastructure → caiman-billing:core, caiman-contracts

caiman-payment:core        → caiman-contracts
caiman-payment:entrypoint  → caiman-payment:core, caiman-web-support
caiman-payment:infrastructure → caiman-payment:core, caiman-contracts

caiman-notification:core   → caiman-contracts
caiman-notification:entrypoint → caiman-notification:core, caiman-contracts
caiman-notification:infrastructure → caiman-notification:core, caiman-contracts

caiman-app → all :entrypoint and :infrastructure modules (composition root — wires everything)
```

Cross-context data access (e.g. billing needing debtor info) uses gateway interfaces defined in `caiman-contracts`, implemented in the providing context's `:infrastructure` module, and wired by `caiman-app`.

---

## Hexagonal Architecture Conventions

### Port naming (`core.port.in` / `core.port.out`)

- **`port.in`** — use case interfaces (primary ports, driven by adapters in `entrypoint`). Named after the action: `CreateDebtorUseCase`, `UpdateDebtorUseCase`.
- **`port.out`** — gateway interfaces (secondary ports, implemented by adapters in `infrastructure`). Named after the capability the core needs: `DebtorPersistenceGateway`, `DebtorEventGateway`, `CreditBureauGateway`.

### Service / use case implementation naming (`core.domain.service`)

- Interface in `port.in`: `CreateDebtorUseCase`
- Implementation in `core.domain.service`: `CreateDebtorService`

### Adapter naming (`infrastructure`)

Suffix is always `Adapter`, never `Impl`. Name by context/intent, not technology — technology changes, intent doesn't.

| Package | Class | Implements |
|---|---|---|
| `infrastructure.database.adapter` | `DebtorPersistenceAdapter` | `DebtorPersistenceGateway` |
| `infrastructure.database.adapter` | `DebtorQueryAdapter` | `DebtorQueryGateway` |
| `infrastructure.messaging.adapter` | `DebtorEventAdapter` | `DebtorEventGateway` |
| `infrastructure.http.adapter` | `CreditBureauAdapter` | `CreditBureauGateway` |

### Command Objects (`core.port.in.command`)

Every `port.in` use case receives a **Command Object** as its input — never a domain model directly.

```
core/port/in/
  CreateDebtorUseCase.java           ← interface
  command/
    CreateDebtorCommand.java         ← input type for the use case
    CreateDebtorContactCommand.java  ← nested data (separate file, not inner class)
```

**Rules:**
- Commands are immutable `record`s with no validation logic and no behavior — pure data transport.
- One command per use case. Commands are never shared between use cases.
- Commands live in `core/port/in/command/`, not inside the service package. The entrypoint maps `RequestDto → Command`; the service maps `Command → domain model` internally.
- The service constructs domain objects from the command using the domain model's builder. MapStruct is not used in `core` — it belongs only in `entrypoint` and `infrastructure` adapters.
- The service validates structural rules (e.g. duplicate `contactType + priority` in a contact list) **before** constructing domain objects, throwing a `BusinessException` → HTTP 422.

**Why commands, not domain objects:**  
If the entrypoint constructs the domain model directly (via mapper), any domain constructor exception fires inside the entrypoint — preventing the service from applying business validation before construction. Commands decouple transport from construction: the service owns when and how domain objects are built.

### Interface segregation

Split `port.out` interfaces when they grow large. Prefer separating write from read:

```
DebtorPersistenceGateway  →  save(Debtor), update(Debtor)
DebtorQueryGateway        →  findById(UUID), findByDocument(String), findAllActive()
```

`CreateDebtorService` only injects `DebtorPersistenceGateway` — it never sees query methods it doesn't use. Each service declares only the ports it actually needs.

### Infrastructure sub-package structure per adapter type

```
infrastructure.database.adapter     ← JPA/DB adapters
infrastructure.messaging.adapter    ← event/message producers
infrastructure.http.adapter         ← outbound HTTP clients
```

---

## Module responsibilities

Detailed description of each Gradle module: business purpose, owned DB tables, events produced/consumed, and REST endpoints exposed.

---

### `caiman-contracts`

**Physical path:** `caiman-shared/contracts/`

**Purpose:** Shared kernel. No business logic, no Spring dependency. Pure Java contracts that cross module boundaries.

- **Base exception** (`exception/`): `CaimanException` (abstract) — all domain exceptions extend this; caught by `caiman-web-support` global handler
- **Domain events** (`event/`, POJOs, no Spring): `InvoiceGeneratedEvent`, `InvoiceOverdueDetectedEvent`, `PendingReminderDueEvent`, `PaymentProofApprovedEvent`, `PaymentProofRejectedEvent`
- **Gateway interfaces** (`gateway/`) consumed cross-context: `DebtorGateway` (provides debtor info to billing), `InvoiceGateway` (provides invoice info to payment)
- **Common types** (`type/`): `Money`, pagination wrappers, shared enums (`CycleUnit`, `InvoiceStatus`, `TriggerType`, etc.)
- **Owns no DB tables**
- **Exposes no endpoints**

---

### `caiman-web-support`

**Physical path:** `caiman-shared/web-support/`

**Purpose:** Web infrastructure shared across all entrypoint modules. Depends on `caiman-contracts` and `spring-web`.

- **`annotation/`**: `@CaimanEndpoint` — meta-annotation combining `@RestController`, `@RequestMapping`, `@Validated`; used by all REST controllers
- **`config/`**: `ControllersConfig` (`WebMvcConfigurer`) — applies path prefix from `CaimanServerProps` to all `@CaimanEndpoint` classes; `GlobalRestExceptionHandlerConfig` (`@ControllerAdvice`) — catches `CaimanException` and subclasses
- **Owns no DB tables**
- **Exposes no endpoints**

---

### `caiman-debtor`

**Purpose:** Manages the lifecycle of debtors — the people who owe money. No billing logic here, only identity and contact data.

**Owned tables:**
- `debtor` — `id`, `name`, `notes`, `notifications_enabled`, `is_active`, `created_at`, `updated_at`
- `debtor_contact` — `id`, `debtor_id`, `contact_type` (`EMAIL`), `contact_value`, `priority`

**Events produced:** none

**Events consumed:** none

**Endpoints exposed:**
- `POST /debtors` — create debtor
- `GET /debtors` — list (active by default, `?includeInactive=true` for all)
- `GET /debtors/{id}` — get by id
- `PATCH /debtors/{id}` — update / deactivate

---

### `caiman-billing`

**Purpose:** Core billing engine. Manages charge plans, memberships, and invoice lifecycle. Runs **Odin** (daily scheduler at 00:00 UTC) which generates invoices, detects overdue status, and schedules reminder notifications via domain events.

**Owned tables:**
- `charge_plan` — `id`, `name`, `description`, `type`, `status`, `proof_validation_mode`, `total_amount`, `due_tolerance_days`, `cycle_interval`, `cycle_unit`, `cycle_anchor_date`, `notifications_enabled`, `notification_time`, `notification_timezone`, `starts_at`, `ends_at`, `end_when_recovered`, `created_at`, `updated_at`
- `charge_plan_notification_config` — `id`, `charge_plan_id`, `trigger_type`, `reminder_interval`, `reminder_unit`, `max_attempts`, `created_at`, `updated_at`
- `charge_plan_member` — `id`, `charge_plan_id`, `debtor_id`, `amount_override`, `rotation_order`, `status`, `credit_balance`, `joined_at`, `left_at`
- `invoice` — `id`, `charge_plan_id`, `charge_plan_member_id`, `cycle_index`, `amount_due`, `amount_paid`, `status`, `due_date`, `cancellation_reason`, `cancelled_at`, `paid_at`, `created_at`, `updated_at`

**Events produced:**
- `InvoiceGeneratedEvent` — after each invoice is created by Odin (one event per invoice)
- `InvoiceOverdueDetectedEvent` — when Odin transitions an invoice to `OVERDUE`
- `PendingReminderDueEvent` — when Odin calculates a pending reminder should be sent today

**Events consumed:**
- `PaymentProofApprovedEvent` — updates `invoice.amount_paid`, transitions invoice to `PAID` or `PARTIALLY_PAID`, cancels scheduled reminder outbox entries if fully paid

**Endpoints exposed:**
- `POST /charge-plans` — create plan
- `GET /charge-plans` — list plans
- `GET /charge-plans/{id}` — get plan
- `PATCH /charge-plans/{id}` — update plan
- `POST /charge-plans/{id}/pause`
- `POST /charge-plans/{id}/resume`
- `POST /charge-plans/{id}/finish`
- `PUT /charge-plans/{id}/notification-config` — upsert notification config per trigger type
- `POST /charge-plans/{id}/members` — add member
- `PUT /charge-plans/{id}/members/reorder` — reorder rotation (ROTATING only)
- `PATCH /charge-plans/{id}/members/{memberId}` — update member (leave, etc.)
- `POST /charge-plans/{id}/members/{memberId}/credit` — add credit balance
- `GET /invoices` — list invoices (filterable by plan, status)
- `GET /invoices/{id}` — get invoice
- `POST /admin/invoices/{id}/cancel` — cancel invoice
- `POST /admin/invoices/{id}/resend-link` — re-enqueue notification link (creates outbox entry via notification gateway)

---

### `caiman-payment`

**Purpose:** Handles payment proof upload, AI-assisted validation, and payment registration. Runs proof analysis asynchronously in a background thread.

**Owned tables:**
- `payment_proof` — `id`, `invoice_id`, `file_path`, `upload_token`, `token_expires_at`, `ai_extracted_value`, `final_value`, `requires_manual_review`, `ai_raw_response`, `status`, `created_at`
- `payment` — `id`, `invoice_id`, `payment_proof_id`, `amount`, `method`, `approved_manually`, `paid_at`, `created_at`

**Events produced:**
- `PaymentProofApprovedEvent` — when proof status transitions to `APPROVED` (via AI or admin)
- `PaymentProofRejectedEvent` — when proof status transitions to `REJECTED`

**Events consumed:** none (triggered by HTTP upload; invoice data fetched via `InvoiceGateway` from `caiman-contracts`)

**Endpoints exposed:**
- `POST /public/invoices/{id}/proof` — unauthenticated, JWT upload token required; debtor submits proof file
- `GET /admin/proofs/pending-review` — list proofs awaiting manual review
- `POST /admin/proofs/{id}/resolve` — admin approves or rejects proof (`{ decision, finalValue }`)
- `POST /admin/invoices/{id}/payments` — admin registers manual payment directly (no proof)

---

### `caiman-notification`

**Purpose:** Owns the notification pipeline end-to-end. Listens to domain events from billing and payment, writes to `notification_outbox`. Runs **Huginn** (minutely scheduler) which reads the outbox and dispatches emails with exponential backoff retry.

**Owned tables:**
- `notification_outbox` — `id`, `invoice_id`, `trigger_type`, `channel`, `recipient`, `payload`, `scheduled_for`, `status`, `attempt_count`, `max_attempts`, `last_attempted_at`, `last_error`, `created_at`
- `notification_log` — `id`, `invoice_id`, `outbox_id`, `trigger_type`, `channel`, `status`, `recipient`, `error_message`, `sent_at`

**Events produced:** none (terminal consumer)

**Events consumed:**
- `InvoiceGeneratedEvent` → creates `INVOICE_CREATED` outbox entry scheduled for `notification_time` today
- `InvoiceOverdueDetectedEvent` → creates `OVERDUE_REMINDER` outbox entry
- `PendingReminderDueEvent` → creates `PENDING_REMINDER` outbox entry (after idempotency guard)
- `PaymentProofApprovedEvent` → creates `PAYMENT_APPROVED` outbox entry with `scheduled_for = NOW()`
- `PaymentProofRejectedEvent` → creates `PAYMENT_REJECTED` outbox entry with `scheduled_for = NOW()`

**Endpoints exposed:** none

---

### `caiman-app`

**Purpose:** Composition root. Wires all modules together. Owns no business logic and no DB tables.

- Contains Spring Boot `main` class and application startup config
- Registers `DebtorGateway`, `InvoiceGateway`, and other cross-module gateway beans (implemented in `:infrastructure` modules, consumed by other contexts)
- Owns security config: static API token filter for admin endpoints, JWT validation filter for public debtor endpoints
- Owns global exception handling (`@ControllerAdvice`)
- Owns `ApplicationEventPublisher` bean used by all modules to publish domain events

---

### Liquibase migrations

**Location:** `caiman-app/src/main/resources/db/` — on the Spring Boot classpath, not a separate Gradle module.

- `db/changelog/db.changelog-master.yaml` — master changelog (`includeAll` over `changes/`)
- `db/changelog/changes/` — one YAML file per table + one for indexes, ordered by FK dependency
  - `V0_01__debtor.yaml` through `V0_11__indexes.yaml`
- Authoritative source for all table and column definitions

---

## Migration ↔ Entity Sync Rule

**Any change to a Liquibase migration (column, constraint, index, rename, drop) must be reflected in the corresponding JPA entity in the same PR/commit.**

- New column → add `@Column` field to entity
- New unique constraint → add `@UniqueConstraint` to `@Table(uniqueConstraints = {...})`
- New index (non-unique) → add `@Index` to `@Table(indexes = {...})`
- Dropped column/constraint/index → remove from entity
- Renamed column → update `@Column(name = ...)` in entity

The entity `@Table` annotation is the Java mirror of the DDL. Keep them identical.

---

## Known Pitfalls & Solutions

Lessons learned during development. Check here before debugging similar issues.

---

### Gradle nested sub-projects with duplicate names cause silent bean loss

**Symptom:** Controllers or beans from bounded-context modules are missing at runtime. Endpoints return 404. No error at startup. Only some modules work.

**Root cause:** Gradle sub-projects named `core`, `entrypoint`, `infrastructure` across multiple bounded contexts share the same project name → identical GAV (`com.caimanproject:entrypoint:0.0.1-SNAPSHOT`). Gradle's conflict resolution picks **one** and silently discards the others. The discarded modules never reach the classpath.

**How to detect:** Run `./gradlew :caiman-app:dependencies --configuration runtimeClasspath` and look for `->` substitution arrows:
```
+--- project :caiman-debtor:entrypoint -> project :caiman-payment:entrypoint   ← wrong
```

**Fix:** In `settings.gradle.kts`, use flat unique project names with `projectDir` mapping. Directory structure stays nested; project identity becomes flat and unique:
```kotlin
include("caiman-debtor-entrypoint")
project(":caiman-debtor-entrypoint").projectDir = file("caiman-debtor/entrypoint")
```
After fix, dependency tree must show NO `->` substitutions between bounded-context modules.

**Related:** Spring Boot 4 requires `@SpringBootApplication(scanBasePackages = "com.caimanproject")` to scan beans across all modules. Without it, only `com.caimanproject.app` is scanned.

---

### Build fails with "Did not resolve" — dependency lockfiles out of date

**Symptom:** Any Gradle task that resolves dependencies (compile, test, bootJar, nativeCompile, etc.) fails with:

```
FAILURE: Build failed with an exception.

* What went wrong:
Could not resolve all files for configuration ':caiman-app:runtimeClasspath'.
> Did not resolve 'org.example:some-lib:4.0.6' which has been forced / substituted to a different version: '4.1.0'
```

**Root cause:** Dependency locking is enabled (`dependencyLocking { lockAllConfigurations() }` in `build.gradle.kts`). Each subproject has a committed `gradle.lockfile` that pins exact versions. When a dependency version changes — via a direct bump in `libs.versions.toml`, a Spring Boot BOM upgrade, or any transitive version shift — the lockfile diverges from the resolved graph. Gradle refuses to proceed.

**Fix:** Regenerate all lockfiles and commit them:

```bash
just security-update-locks
# or directly:
./gradlew updateDependencyLocks --write-locks -q
```

Then commit every `gradle.lockfile` that changed alongside the dependency change that triggered this.

**Note:** This error is intentional — it means a dependency version changed without explicit acknowledgment. The lockfile is the security control that prevents unvetted dependency upgrades from silently entering the build.

---

### Domain model builders must use `restoreBuilder` / `createBuilder` naming

**Rule:** Domain model classes that need two Lombok builders (one for DB restoration, one for creation) **must** follow this exact naming convention:

```java
@Builder(builderMethodName = "restoreBuilder")   // full constructor — used by infra mappers (DB → domain)
public DomainClass(UUID id, ..., Audit audit) { ... }

@Builder(builderMethodName = "createBuilder")    // creation constructor — used by services
public DomainClass(String name, ...) { ... }
```

**Why:** `caiman-shared/mapper-spi` contains a custom `CaimanBuilderProvider` (MapStruct SPI) that intercepts `MoreThanOneBuilderCreationMethodException` and explicitly selects `restoreBuilder()` for infra mappers. Any other name for the DB-restore builder will cause MapStruct to fail with ambiguous constructor errors at compile time.

**Scope:** `caiman-mapper-spi` is registered as `annotationProcessor` only in `:infrastructure` modules. The SPI resolves ambiguity specifically for the infra layer, which maps entities → domain models via `restoreBuilder`. The `createBuilder` is used only by services in `core` — never by mappers.

---

## GitHub Actions — Composite Action Wrappers

Every third-party GitHub Action used in this project **must** be wrapped in a local Composite Action. Direct use of external actions in workflow files is not allowed.

### Rule

1. Create `.github/actions/<wrapper-name>/action.yml` — a composite action that delegates to the external action with a **SHA-pinned** ref.
2. Workflows reference the local wrapper (`uses: ./.github/actions/<wrapper-name>`), never the external action directly.
3. The SHA lives in exactly one file (the wrapper). Updating the external action = changing one line in one file.

**Exception — `actions/checkout`:** Local composite actions can only be resolved after the repository is checked out. Since `actions/checkout` is always the first step of every job (the bootstrap step), it cannot be wrapped — the wrapper file wouldn't exist on disk yet. Use `actions/checkout@<SHA> #vX.Y.Z` directly in every workflow job's first step.

**Why SHA pins, not tags:** Tags are mutable — an attacker or maintainer can move them without warning. A SHA is immutable. Tags are allowed only inside the wrapper as a human-readable comment (`#v1.4.4`), never as the functional ref.

### Wrapper structure

```yaml
name: Setup GraalVM
description: Wraps graalvm/setup-graalvm with a pinned SHA.

inputs:
  github-token:
    required: true
    description: GitHub token used for GraalVM setup and cache authentication

runs:
  using: composite
  steps:
    - uses: graalvm/setup-graalvm@790e28947b79a9c09c3391c0f18bf8d0f102ed69 #v1.4.4
      with:
        java-version: '25'
        distribution: 'graalvm'
        github-token: ${{ inputs.github-token }}
        cache: 'gradle'
```

Caller in workflow:
```yaml
- uses: ./.github/actions/setup-graalvm
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
```

### Current wrappers

| Wrapper | Wraps | SHA comment |
|---|---|---|
| *(none — used directly)* | `actions/checkout` | #v6.0.0 — exception, see above |
| `.github/actions/setup-graalvm` | `graalvm/setup-graalvm` | #v1.4.4 |
| `.github/actions/cache` | `actions/cache` | #v5.0.5 |
| `.github/actions/install-cosign` | `sigstore/cosign-installer` | #v4.1.2 |
| `.github/actions/upload-artifact` | `actions/upload-artifact` | #v5.0.0 |
| `.github/actions/download-artifact` | `actions/download-artifact` | #v6.0.0 |
| `.github/actions/create-github-release` | `softprops/action-gh-release` | #v2.2.0 |
| `.github/actions/docker-login` | `docker/login-action` | #v3.6.0 |
| `.github/actions/setup-anchore-tools` | Anchore Syft + Grype (custom download + verify) | — |
| `.github/actions/install-just` | `taiki-e/install-action` | #v2.82.0 |
| `.github/actions/send-mail` | `dawidd6/action-send-mail` | #v17 |

### Adding a new external action

1. Create `.github/actions/<name>/action.yml` with the SHA-pinned `uses:` and the tag as comment.
2. Expose only the inputs/outputs that callers actually need.
3. Every input must include a `description` field — without it the wrapper is unusable as self-documentation.
4. Reference the wrapper from all workflow files.
5. Never reference the external action directly in a workflow.

### Updating an external action

1. Find the new commit SHA on the action's GitHub page (`git log` or the releases page).
2. Update the single `uses:` line in `.github/actions/<name>/action.yml`.
3. Update the tag comment on the same line.
4. No workflow file changes needed.

---

## Documentation — always read before implementing

All business decisions, domain rules, and technical choices are documented in `./docs`. **Always consult the relevant documentation before writing or modifying any code.**

| File | Purpose |
| :---- | :---- |
| `./docs/BUSINESS_RULES.md` | **Primary reference.** Contains the complete project description, glossary, version roadmap, technology stack decisions, and all business rules and processing flows. Read this first. |
| `./docs/diagrams/erd.mmd` | Entity-relationship diagram — all tables, columns, and relationships |
| `./docs/diagrams/state-invoice.mmd` | Invoice state machine |
| `./docs/diagrams/state-payment-proof.mmd` | PaymentProof state machine |
| `./docs/diagrams/state-notification-outbox.mmd` | NotificationOutbox state machine |
| `./docs/diagrams/full-flow.mmd` | Full system flow from ChargePlan creation to payment result notification |
| `./caiman-app/src/main/resources/db/changelog/changes/` | Complete Liquibase DDL — one file per table (V0_01–V0_11), authoritative source for table and column definitions |

## Key concepts (quick reference)

- **Debtor**: a person who owes money. No system login. Interacts via tokenized email links only.  
- **ChargePlan**: the core billing entity. Type `ROTATING` (one payer per cycle, rotating order) or `SPLIT` (all members pay each cycle).  
- **Invoice**: a single charge issued to one member for one cycle. Generated by **Odin**.  
- **Odin**: the daily scheduler job (`@Scheduled`, runs at 00:00 UTC). Generates invoices, detects overdue status, enqueues notifications.  
- **Huginn**: the minutely dispatcher job (`@Scheduled`, runs every minute). Reads `notification_outbox` and dispatches notifications with exponential backoff retry. Deletes outbox rows on terminal state.  
- **PaymentProof**: receipt uploaded by debtor. Processed according to `charge_plan.proof_validation_mode`: `AI_AUTO`, `MANUAL`, or `AI_ASSISTED`.  
- **NotificationOutbox**: transactional outbox — work queue only. Rows are deleted after dispatch (success or exhaustion). History lives in `notification_log`.

## Critical implementation rules

1. **Spring dependency boundary in `core` modules:** `core` may depend on `spring-context` (`@Service`, `@Component`) and `spring-tx` (`@Transactional`) for DI and transaction demarcation. No `spring-web`, `spring-data-jpa`, `spring-mail`, or any infrastructure library is allowed in `core`. All infra dependencies belong in `infrastructure` or `entrypoint` sub-modules.

2. **All timestamps are UTC.** Force `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))` at startup. Configure Hibernate with `hibernate.jdbc.time_zone=UTC`. Never use `NOW()` or `CURRENT_TIMESTAMP` in business logic queries — always pass explicit `Instant` values from the application.

3. **Every table must have `created_at` and `updated_at` columns** (`datetime`, `nullable: false`). On insert, the application sets both to the same `Instant`. On update, the application sets `updated_at`. No database defaults — the application is always the source of these values (see rule 4).

4. **Never use database-side date/time defaults** (`defaultValueComputed: now()`, `CURRENT_TIMESTAMP`, etc.) on any column. The database server may run in a different timezone than the application, producing silent timezone drift. The application always provides explicit `Instant` values on insert and update.
5. **All UUIDs are generated by the application**, never by the database.  
6. **Monetary values use `DECIMAL(15,2)` with `HALF_UP` rounding.**  
7. **No hard deletes.** Use `is_active`, `status = LEFT`, `status = FINISHED`, or `status = CANCELLED`.  
8. **Odin never sends notifications. Huginn never generates invoices.** These jobs have strictly separate responsibilities.  
9. **Outbox entries for the same `invoice_id` \+ `trigger_type` must not be duplicated.** Odin always checks for existing `SCHEDULED` or `PROCESSING` entries before enqueuing (idempotency guard).  
10. **Reminder scheduling is calculated from `invoice.created_at` (PENDING\_REMINDER) and `invoice.due_date` (OVERDUE\_REMINDER).** Odin does not consult `notification_log` to decide whether to enqueue — `notification_log` is audit-only.  
11. **`PAYMENT_APPROVED` and `PAYMENT_REJECTED` notifications are enqueued inline** at proof resolution time with `scheduled_for = NOW()`, not by Odin.

12. **Dependency versions must be centralized in `gradle/libs.versions.toml`.** Before adding any dependency, check whether it is managed by the Spring Boot BOM. If it is, declare it without a version. If not, put the version in `libs.versions.toml`.

    **How to check Spring Boot BOM coverage** (always check these before adding a version):
    - https://docs.spring.io/spring-boot/appendix/dependency-versions/properties.html — shows the property name that controls the version
    - https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html — lists every `groupId:artifactId` managed by the BOM with its version; prefer what is declared here

    - **BOM-managed** (Spring Boot starters, Lombok, JPA, Hibernate ORM, etc.): declare without version in the module build file. BOM is the source of truth.
    - **Non-BOM-managed** (MapStruct, lombok-mapstruct-binding, and any third-party lib not covered by a BOM): version goes in `gradle/libs.versions.toml`, referenced via `libs.*` accessor in the module build file.
    - **Gradle plugins**: always declared in `gradle/libs.versions.toml` under `[plugins]` and referenced via `alias(libs.plugins.*)` in all build files. Plugin versions cannot be managed by the BOM — inline the version directly in the `[plugins]` entry (e.g. `hibernate-orm = { id = "org.hibernate.orm", version = "7.2.12.Final" }`) and keep it in sync with the BOM's managed version.

13. **Every test class must be annotated with `@UnitTest` or `@IntegrationTest`** (from `com.caimanproject.test.annotation`). These annotations drive the `./gradlew unitTest` and `./gradlew integrationTest` Gradle tasks.

    - `@UnitTest` — no Spring context, no infrastructure. Typically `@ExtendWith(MockitoExtension.class)` only.
    - `@IntegrationTest` — boots a Spring context (`@SpringBootTest`) or uses Testcontainers. `@IntegrationTest` is `@Inherited`: annotating an abstract base class (e.g. `IntegrationTestController`) propagates the tag to all subclasses — do **not** repeat it on the subclass.

    ```java
    // Unit test — annotate the class directly
    @UnitTest
    @ExtendWith(MockitoExtension.class)
    class DebtorServiceTest { ... }

    // Integration test base — annotation inherited by all subclasses
    @IntegrationTest
    @SpringBootTest(...)
    public abstract class IntegrationTestController { ... }

    // Subclass — NO @IntegrationTest needed, inherited from base
    class DebtorControllerIT extends IntegrationTestController { ... }

    // Integration test with no base — annotate directly
    @IntegrationTest
    @SpringBootTest(...)
    class CreateDebtorSQLiteIT { ... }
    ```

    Run commands: `just test-unit`, `just test-integration-jvm`, `just test` (both JVM), `just test-integration-native` (GraalVM native, requires Docker).

14. **No wildcard imports (`*`).** All imports must be fully qualified. The only exception is test source files, where wildcard imports are allowed exclusively for assertion and mock libraries.

    **Forbidden everywhere (including tests):**
    ```java
    import java.util.*;
    import org.springframework.web.bind.annotation.*;
    import com.caimanproject.billing.core.domain.*;
    ```

    **Required — always use full imports:**
    ```java
    import java.util.List;
    import java.util.UUID;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RestController;
    import com.caimanproject.billing.core.domain.Invoice;
    ```

    **Allowed only in test files — assertion and mock libs only:**
    ```java
    // AssertJ
    import static org.assertj.core.api.Assertions.*;
    // Mockito
    import static org.mockito.Mockito.*;
    import static org.mockito.ArgumentMatchers.*;
    // JUnit 5
    import static org.junit.jupiter.api.Assertions.*;
    ```

15. **GraalVM native image.** This project compiles to a native binary via `./gradlew :caiman-app:nativeCompile`. Every implementation decision must account for native-image constraints. GraalVM's static analysis cannot see reflection, `Class.forName()`, dynamic proxies, or resource loading that happens at runtime — these must be declared upfront as hints.

    **Reflection and class loading — what to watch for:**
    - Any class instantiated via `Class.forName()` or loaded by name through a framework (e.g. Hibernate loading a dialect by its fully qualified class name) must be registered in a `RuntimeHintsRegistrar`.
    - Any class whose fields or methods are accessed via `Method.invoke()` / `Field.get()` at runtime (e.g. Hibernate Validator traversing `@ConfigurationProperties` records) requires explicit `MemberCategory` registration.
    - Dynamic JDK proxies and CGLIB proxies must be declared via `hints.proxies()`.
    - Resources loaded via `ClassLoader.getResourceAsStream()` must be declared via `hints.resources()`.

    **Adding a new dependency — mandatory compatibility check:**
    Before adding any library, verify native-image support using these signals (in order of preference):
    1. `META-INF/native-image/` directory inside the JAR — self-contained support, no extra work needed.
    2. Listed in the [GraalVM Reachability Metadata Repository](https://github.com/oracle/graalvm-reachability-metadata) — the `org.graalvm.buildtools.native` plugin downloads the metadata automatically at build time.
    3. Neither of the above — manual hints registration is required before the native binary will work. Factor this into the implementation.

    **`*AotConfig.java` — mandatory reflection registration:**
    Each bounded-context `:entrypoint` module owns a `*AotConfig.java` class in its `aot` package that registers all classes requiring reflection. Use `@RegisterReflectionForBinding` grouped by purpose:

    ```java
    // caiman-debtor/entrypoint/aot/DebtorAotConfig.java
    @Configuration(proxyBeanMethods = false)
    @RegisterReflectionForBinding({
        // DTOs — Jackson serialization/deserialization
        CreateDebtorRequestDto.class,
        DebtorResponseDto.class,
        // ConstraintValidators — SpringConstraintValidatorFactory instantiates via reflection (no-arg constructor).
        ContactValueValidator.class
    })
    public class DebtorAotConfig {}
    ```

    Rules:
    - Every new DTO added to an `:entrypoint` module must also be added to that module's `*AotConfig.java`.
    - Every custom `ConstraintValidator` must also be registered — Spring AOT does not auto-register them. `SpringConstraintValidatorFactory` instantiates them via reflection and will throw `NoSuchMethodException` at runtime without registration.
    - `@RegisterReflectionForBinding` on a `@Configuration` is picked up automatically by Spring AOT. Multiple such classes across modules are aggregated — no central registry is needed.
    - `ErrorResponseDto` (in `caiman-web-support`) is registered directly on `GlobalRestExceptionHandlerConfig` — that is the exception because the DTO and the config that uses it live in the same shared module.

    **`CaimanRuntimeHints` (`caiman-app/.../aot/CaimanRuntimeHints.java`):**
    Used exclusively for hints that cannot be expressed via `@RegisterReflectionForBinding`:
    - `CaimanServerPropsConfig` nested records — Hibernate Validator's `JPATraversableResolver` accesses private fields at startup; Spring AOT alone does not register `ACCESS_DECLARED_FIELDS` for these.
    - `SQLiteDialect` — `hibernate-community-dialects` has no native-image metadata; Hibernate loads the dialect by class name via `ClassLoaderServiceImpl.classForName()`.

    Any future case where a class must be loaded by name at runtime and the owning library has no native-image support belongs here.

