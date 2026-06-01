# Caiman — Agent Instructions

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
    core/                             ← Domain model, port interfaces (no Spring)
    entrypoint/                       ← Adapters/in: REST controllers
    infrastructure/                   ← Adapters/out: JPA repositories
  caiman-billing/                     ← Bounded context: ChargePlan, Member, Invoice, Odin scheduler
    core/                             ← Domain model, port interfaces (no Spring)
    entrypoint/                       ← Adapters/in: REST controllers, OdinJob (@Scheduled)
    infrastructure/                   ← Adapters/out: JPA repositories, event publisher
  caiman-payment/                     ← Bounded context: PaymentProof, Payment, AI analysis
    core/                             ← Domain model, port interfaces (no Spring)
    entrypoint/                       ← Adapters/in: REST controllers (public proof upload)
    infrastructure/                   ← Adapters/out: JPA repositories, Anthropic API client
  caiman-notification/                ← Bounded context: NotificationOutbox, NotificationLog, Huginn
    core/                             ← Domain model, port interfaces (no Spring)
    entrypoint/                       ← Adapters/in: HuginnJob (@Scheduled), domain event listeners
    infrastructure/                   ← Adapters/out: JPA repositories, SMTP email sender
  caiman-app/                         ← Spring Boot main class, security config, composition root
  db/                                 ← Liquibase changelog files
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
| `./db/changelog/changes/V1__initial_schema.yaml` | Complete Liquibase DDL — authoritative source for table and column definitions |

## Key concepts (quick reference)

- **Debtor**: a person who owes money. No system login. Interacts via tokenized email links only.  
- **ChargePlan**: the core billing entity. Type `ROTATING` (one payer per cycle, rotating order) or `SPLIT` (all members pay each cycle).  
- **Invoice**: a single charge issued to one member for one cycle. Generated by **Odin**.  
- **Odin**: the daily scheduler job (`@Scheduled`, runs at 00:00 UTC). Generates invoices, detects overdue status, enqueues notifications.  
- **Huginn**: the minutely dispatcher job (`@Scheduled`, runs every minute). Reads `notification_outbox` and dispatches notifications with exponential backoff retry. Deletes outbox rows on terminal state.  
- **PaymentProof**: receipt uploaded by debtor. Processed according to `charge_plan.proof_validation_mode`: `AI_AUTO`, `MANUAL`, or `AI_ASSISTED`.  
- **NotificationOutbox**: transactional outbox — work queue only. Rows are deleted after dispatch (success or exhaustion). History lives in `notification_log`.

## Critical implementation rules

1. **All timestamps are UTC.** Force `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))` at startup. Configure Hibernate with `hibernate.jdbc.time_zone=UTC`. Never use `NOW()` or `CURRENT_TIMESTAMP` in business logic queries — always pass explicit `Instant` values from the application.  
2. **All UUIDs are generated by the application**, never by the database.  
3. **Monetary values use `DECIMAL(15,2)` with `HALF_UP` rounding.**  
4. **No hard deletes.** Use `is_active`, `status = LEFT`, `status = FINISHED`, or `status = CANCELLED`.  
5. **Odin never sends notifications. Huginn never generates invoices.** These jobs have strictly separate responsibilities.  
6. **Outbox entries for the same `invoice_id` \+ `trigger_type` must not be duplicated.** Odin always checks for existing `SCHEDULED` or `PROCESSING` entries before enqueuing (idempotency guard).  
7. **Reminder scheduling is calculated from `invoice.created_at` (PENDING\_REMINDER) and `invoice.due_date` (OVERDUE\_REMINDER).** Odin does not consult `notification_log` to decide whether to enqueue — `notification_log` is audit-only.  
8. **`PAYMENT_APPROVED` and `PAYMENT_REJECTED` notifications are enqueued inline** at proof resolution time with `scheduled_for = NOW()`, not by Odin.

