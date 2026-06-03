# Caiman — Agent Instructions

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
- **Database**: PostgreSQL (primary), SQLite (fallback)  
- **Migrations**: Liquibase (YAML changelogs)  
- **AI**: Anthropic API (claude-sonnet model) for payment proof analysis  
- **Build**: Gradle (multi-project, Kotlin DSL)  
- **Deployment**: Docker Compose

## Project structure

```
caiman-server/                        ← Gradle root project
  caiman-shared/                      ← Domain events, cross-module gateway interfaces, common types
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

**No cross-module Maven/Gradle dependencies between bounded contexts.** All cross-context communication goes through `caiman-shared`.

```
caiman-shared
  └── no dependencies on other modules

caiman-debtor:core         → caiman-shared
caiman-debtor:entrypoint   → caiman-debtor:core
caiman-debtor:infrastructure → caiman-debtor:core, caiman-shared

caiman-billing:core        → caiman-shared
caiman-billing:entrypoint  → caiman-billing:core
caiman-billing:infrastructure → caiman-billing:core, caiman-shared

caiman-payment:core        → caiman-shared
caiman-payment:entrypoint  → caiman-payment:core
caiman-payment:infrastructure → caiman-payment:core, caiman-shared

caiman-notification:core   → caiman-shared
caiman-notification:entrypoint → caiman-notification:core, caiman-shared
caiman-notification:infrastructure → caiman-notification:core, caiman-shared

caiman-app → all :entrypoint and :infrastructure modules (composition root — wires everything)
```

Cross-context data access (e.g. billing needing debtor info) uses gateway interfaces defined in `caiman-shared`, implemented in the providing context's `:infrastructure` module, and wired by `caiman-app`.

## Module responsibilities

Detailed description of each Gradle module: business purpose, owned DB tables, events produced/consumed, and REST endpoints exposed.

---

### `caiman-shared`

**Purpose:** Shared kernel. No business logic. Contains only contracts that cross module boundaries.

- **Domain events** (POJOs, no Spring): `InvoiceGeneratedEvent`, `InvoiceOverdueDetectedEvent`, `PendingReminderDueEvent`, `PaymentProofApprovedEvent`, `PaymentProofRejectedEvent`
- **Gateway interfaces** consumed cross-context: `DebtorGateway` (provides debtor info to billing), `InvoiceGateway` (provides invoice info to payment)
- **Common types**: `Money`, pagination wrappers, shared enums (`CycleUnit`, `InvoiceStatus`, `TriggerType`, etc.)
- **Owns no DB tables**
- **Exposes no endpoints**

---

### `caiman-debtor`

**Purpose:** Manages the lifecycle of debtors — the people who owe money. No billing logic here, only identity and contact data.

**Owned tables:**
- `debtor` — `id`, `name`, `notes`, `notifications_enabled`, `is_active`, `created_at`, `updated_at`
- `debtor_contact` — `id`, `debtor_id`, `contact_type` (`EMAIL`, `MOBILE_PHONE`, `WHATSAPP`, `TELEGRAM`), `contact_value`, `priority`

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

**Events consumed:** none (triggered by HTTP upload; invoice data fetched via `InvoiceGateway` from `caiman-shared`)

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
3. **All UUIDs are generated by the application**, never by the database.  
4. **Monetary values use `DECIMAL(15,2)` with `HALF_UP` rounding.**  
5. **No hard deletes.** Use `is_active`, `status = LEFT`, `status = FINISHED`, or `status = CANCELLED`.  
6. **Odin never sends notifications. Huginn never generates invoices.** These jobs have strictly separate responsibilities.  
7. **Outbox entries for the same `invoice_id` \+ `trigger_type` must not be duplicated.** Odin always checks for existing `SCHEDULED` or `PROCESSING` entries before enqueuing (idempotency guard).  
8. **Reminder scheduling is calculated from `invoice.created_at` (PENDING\_REMINDER) and `invoice.due_date` (OVERDUE\_REMINDER).** Odin does not consult `notification_log` to decide whether to enqueue — `notification_log` is audit-only.  
9. **`PAYMENT_APPROVED` and `PAYMENT_REJECTED` notifications are enqueued inline** at proof resolution time with `scheduled_for = NOW()`, not by Odin.

10. **Dependency versions must be centralized in `gradle/libs.versions.toml`.** Any dependency whose version is **not** managed by a BOM (Spring Boot BOM or another imported BOM) must declare its version in the catalog — never hardcode a version string directly in a `build.gradle.kts` module file.

    - **BOM-managed** (Spring Boot starters, Lombok, JPA, etc.): declare without version in the module build file. BOM is the source of truth.
    - **Non-BOM-managed** (MapStruct, lombok-mapstruct-binding, and any third-party lib not covered by a BOM): version goes in `gradle/libs.versions.toml`, referenced via `libs.*` accessor in the module build file.
    - **Gradle plugins**: always declared in `gradle/libs.versions.toml` under `[plugins]` and referenced via `alias(libs.plugins.*)` in the root `build.gradle.kts`.

11. **No wildcard imports (`*`).** All imports must be fully qualified. The only exception is test source files, where wildcard imports are allowed exclusively for assertion and mock libraries.

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

