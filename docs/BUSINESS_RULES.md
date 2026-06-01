# Caiman — Project Reference & Business Rules

**This document is the authoritative reference for the Caiman project.** It is intended to be used by AI agents, developers, and contributors as the single source of truth for what the system is, what it does, and how it behaves. All implementation decisions must be consistent with this document.

---

## Table of Contents

- [Project Overview](#project-overview)  
- [Core Concepts & Glossary](#core-concepts--glossary)  
- [Architecture Overview](#architecture-overview)  
- [Version Roadmap](#version-roadmap)  
  - [Version 0 — Beta](#version-0--beta)  
  - [Version 1 — Release](#version-1--release)  
- [Technology Stack](#technology-stack)  
- [Business Rules](#business-rules)  
  1. [General Conventions](#1-general-conventions)  
  2. [Debtor Management](#2-debtor-management)  
  3. [Charge Plan Management](#3-charge-plan-management)  
  4. [Charge Plan Member Management](#4-charge-plan-member-management)  
  5. [Invoice Generation](#5-invoice-generation)  
  6. [Invoice Lifecycle](#6-invoice-lifecycle)  
  7. [Payment Proof — Upload Flow](#7-payment-proof--upload-flow)  
  8. [Payment Proof — AI Analysis Flow](#8-payment-proof--ai-analysis-flow)  
  9. [Payment Registration](#9-payment-registration)  
  10. [Notification Scheduling — Odin](#10-notification-scheduling--odin)  
  11. [Notification Dispatch — Huginn](#11-notification-dispatch--huginn)  
  12. [Admin Operations](#12-admin-operations)  
  13. [Charge Plan Termination](#13-charge-plan-termination)  
  14. [Validation Rules Summary](#14-validation-rules-summary)  
- [Appendix A — Missing Field Identified During Rule Writing](#appendix-a--missing-field-identified-during-rule-writing)

---

## Project Overview

**Caiman** is an open-source, self-hosted personal billing management system designed for individuals who need to track and collect informal recurring or one-time shared expenses from a small group of people.

### The Problem It Solves

Existing tools fall into two categories that do not serve this use case:

- **Expense splitters** (Splitwise, SplitPro, ihatemoney): designed for splitting one-time shared expenses between friends. They lack recurring billing, dunning logic, or any concept of a formal collection flow.  
- **Subscription billing platforms** (Lago, Kill Bill, UniBee): designed for businesses charging customers via integrated payment gateways. They require payment infrastructure, are heavyweight to self-host, and are not designed for informal, proof-based payment confirmation.

Caiman fills the gap: **recurring and one-time informal billing between a single admin and a small group of debtors, with payment confirmation via receipt upload and AI-assisted validation.**

### Primary Use Cases

**Recurring shared subscription (ROTATING plan)**

"I share a YouTube Premium family plan with 5 friends. Every month a different person pays the full subscription. I want the system to automatically track whose turn it is, send them a reminder, and let them submit a payment proof so I know they paid."

**One-time shared expense (SPLIT plan)**

"I booked an AirBnB for a group trip that cost R$ 4,000. Four other people owe me their share. I want to split the amount, send each person a charge, and track who has paid and who hasn't until the full amount is recovered."

### Design Principles

- **Admin-centric**: only the admin (the person self-hosting the system) has a login. Debtors interact only via tokenized links received by email — they never create accounts.  
- **Payment-method agnostic**: the system does not integrate with any payment gateway. Payment is confirmed by uploading a proof (PIX receipt, bank transfer screenshot, Venmo screenshot, etc.). AI validates the proof.  
- **Self-hosted first**: designed to run on a personal server (e.g. home Unraid server, VPS, Raspberry Pi) with minimal resource requirements. Docker Compose is the primary deployment method.  
- **Open source**: the project is fully open-source. The community can contribute support for additional databases, notification channels, and payment methods.

---

## Core Concepts & Glossary

| Term | Definition |
| :---- | :---- |
| **Admin** | The person who owns and operates the Caiman instance. Has full access to all system features via the admin UI. |
| **Debtor** | A person who owes money in one or more charge plans. Does not have a system login. Interacts only via tokenized email links. |
| **ChargePlan** | The core entity. Defines a billing arrangement: who owes, how much, how often, and when it ends. Two subtypes: `ROTATING` and `SPLIT`. |
| **ROTATING plan** | A charge plan where only one member pays per cycle, rotating in a defined order. Example: shared YouTube Premium where each person pays one month per cycle. |
| **SPLIT plan** | A charge plan where all members are charged each cycle, splitting the total amount. Example: AirBnB trip split among 5 people. |
| **ChargePlanMember** | The association between a `Debtor` and a `ChargePlan`. Holds per-member config (amount override, rotation order, credit balance). |
| **Invoice** | A single billing record issued to one member for one billing cycle. Generated automatically by the scheduler. |
| **Payment** | A confirmed payment event linked to an invoice. An invoice can have multiple payments (partial payment support). |
| **PaymentProof** | An uploaded receipt submitted by a debtor via a tokenized public link. Processed according to the plan's `proof_validation_mode`: automatically by AI (`AI_AUTO`), by admin only (`MANUAL`), or AI-analyzed then admin-confirmed (`AI_ASSISTED`). |
| **NotificationOutbox** | Transactional outbox table. The scheduler writes pending notifications here; the dispatcher reads and sends them. Enables retry logic. |
| **NotificationLog** | Immutable append-only audit log of every notification dispatch attempt. |
| **Odin** | The daily scheduler job (`OdinJob`), runs once at `00:00 UTC`. Generates invoices, detects overdue status, and enqueues notifications into the `notification_outbox`. Named after the Norse god Odin — the all-seeing figure who surveys everything under his watch once a day, makes decisions, and sets events in motion. Odin observes, decides, and dispatches; he does not deliver messages himself — that is Huginn's role. |
| **Huginn** | The minutely dispatcher job (`HuginnJob`), runs every minute. Reads `SCHEDULED` entries from `notification_outbox` and dispatches notifications with exponential backoff retry logic. Named after one of Odin's two ravens — Huginn (thought) — who flies across the world carrying messages and always completes his mission. Thematically paired with Odin: Odin decides and enqueues; Huginn executes and delivers. |
| **Upload Token** | A short-lived JWT (48h) embedded in the notification email link. Authorizes a debtor to upload a proof for a specific invoice without requiring a login. |

---

## Architecture Overview

Caiman is composed of two services deployed together via Docker Compose:

┌─────────────────────────────────┐     ┌─────────────────────────────────┐

│         caiman-backend          │     │         caiman-frontend          │

│                                 │     │                                  │

│  Spring Boot 4 / Java 25        │◄────│  Spring Boot 4 (serves React)   │

│  REST API                       │     │  React SPA (admin UI)            │

│  Scheduler (Odin & Huginn)      │     │  Public proof upload pages       │

│  AI integration (Anthropic API) │     │                                  │

└────────────────┬────────────────┘     └─────────────────────────────────┘

                 │

        ┌────────┴────────┐

        │                 │

   ┌────┴────┐      ┌─────┴─────┐

   │Postgres │  or  │  SQLite   │

   │(primary)│      │(fallback) │

   └─────────┘      └───────────┘

**Backend** (`caiman-backend`):

- Exposes all REST endpoints consumed by the frontend and by the public debtor-facing pages.  
- Runs Odin (daily scheduler) and Huginn (minutely dispatcher) as Spring `@Scheduled` tasks.  
- Integrates with the Anthropic API for proof analysis.  
- Integrates with SMTP for email notifications.

**Frontend** (`caiman-frontend`):

- Spring Boot application that serves a compiled React SPA for the admin UI.  
- Also serves the unauthenticated public pages: proof upload page and proof confirmation page.  
- Communicates with the backend exclusively via the REST API.

**Authentication:**

- Admin access is protected by a static API token configured at application startup via environment variable (`CAIMAN_API_TOKEN`). The frontend passes this token in every request to the backend.  
- Debtor-facing public endpoints are protected by short-lived JWT upload tokens embedded in email links. No login required.

---

## Version Roadmap

### Version 0 — Beta

**Goal:** fully functional core system usable by the author in production for real personal use cases (YouTube Premium rotating plan, AirBnB-style splits). No polish, no onboarding, limited error handling UI.

**Included in v0:**

- [ ] `Debtor` CRUD (create, read, update, deactivate)  
- [ ] `ChargePlan` CRUD — both `ROTATING` and `SPLIT` types  
- [ ] `ChargePlanMember` management — add, reorder (ROTATING), leave with optional invoice cancellation  
- [ ] Manual credit balance via admin endpoint  
- [ ] `Invoice` generation — Odin daily scheduler  
- [ ] Invoice overdue detection — Odin  
- [ ] Invoice status machine — full lifecycle  
- [ ] Partial payment support  
- [ ] `NotificationOutbox` \+ `NotificationLog` — transactional outbox pattern  
- [ ] Huginn notification dispatcher with exponential backoff retry  
- [ ] Three notification trigger types: `INVOICE_CREATED`, `PENDING_REMINDER`, `OVERDUE_REMINDER`  
- [ ] Email notifications via SMTP (configurable host, port, credentials)  
- [ ] Public tokenized proof upload endpoint (JWT, 48h expiry)  
- [ ] AI-assisted proof validation via Anthropic API  
- [ ] User confirmation step on the public proof page (Opção C flow)  
- [ ] Divergence detection → manual review queue  
- [ ] Manual payment registration by admin  
- [ ] Manual proof review resolution by admin (approve / reject)  
- [ ] Admin resend link endpoint  
- [ ] Admin invoice cancellation  
- [ ] ChargePlan termination — by date, by recovered amount, manual  
- [ ] Static token authentication for admin endpoints  
- [ ] Database support: PostgreSQL (primary) and SQLite (fallback)  
- [ ] Liquibase migrations  
- [ ] Docker Compose deployment with PostgreSQL  
- [ ] Basic admin UI: Debtors list, ChargePlan list, Invoice list per plan, Payment proof review queue

**Not included in v0 (explicitly out of scope):**

- Debtor portal / dashboard (debtors cannot log in)  
- Multi-language support  
- Multiple notification channels (Telegram, SMS)  
- Payment anticipation / future invoice prepayment  
- Automatic credit-from-overpayment conversion  
- Advanced reporting / charts

---

### Version 1 — Release

**Goal:** polished, documented, community-ready release. Suitable for others to self-host with confidence.

**Planned additions in v1 (not fully specified — open for design):**

- [ ] **Debtor portal**: a lightweight authenticated area (login by email \+ magic link or simple password) where a debtor can see all their open and paid invoices across all plans, and upload a proof without needing the email link. Eliminates the "link expired" problem.  
- [ ] **Dashboard**: admin home screen with summary widgets — total outstanding, total received this month, number of overdue invoices, plans active/paused/finished.  
- [ ] **Telegram bot notifications**: optional secondary channel alongside email. Debtor provides Telegram handle; admin configures the bot token. Requires a proper bot setup guide in the docs.  
- [ ] **Payment anticipation**: debtor can pre-pay future invoices. Excess amount is automatically converted to `credit_balance` on their membership.  
- [ ] **Multi-language / i18n**: email templates and UI in at least Portuguese (BR) and English.  
- [ ] **Improved public proof page UX**: progress steps, clearer error messages, mobile-optimized layout.  
- [ ] **Audit log**: admin-visible log of all state changes (who changed what, when) for invoices and plans.  
- [ ] **Reporting**: basic charts — monthly received vs expected, per-debtor payment history, per-plan recovery progress.  
- [ ] **MySQL support**: community-contributed database support as a third option alongside PostgreSQL and SQLite.  
- [ ] **Full documentation site**: getting started guide, configuration reference, deployment guide (bare metal, Docker, Unraid community app).  
- [ ] **Automated tests**: minimum coverage target for backend service layer and scheduler logic.

---

## Technology Stack

| Component | Technology |
| :---- | :---- |
| Backend language | Java 25 |
| Backend framework | Spring Boot 4 |
| Database (primary) | PostgreSQL |
| Database (fallback) | SQLite |
| Database migrations | Liquibase (YAML changelogs) |
| ORM | Hibernate / Spring Data JPA |
| Scheduler | Spring `@Scheduled` |
| Authentication | Static API token (admin) \+ JWT (public debtor links) |
| AI provider | Anthropic API (claude-sonnet model) |
| Email | SMTP via Spring Mail (configurable) |
| Frontend framework | React (served by a Spring Boot app) |
| Deployment | Docker Compose |

**Key technical decisions:**

- All timestamps stored as UTC in the database. `TIMESTAMPTZ` on PostgreSQL, `TEXT` ISO 8601 on SQLite. Liquibase `datetime` type handles the mapping automatically.  
- UUIDs stored as `VARCHAR(36)`. Generated by the application, never by the database.  
- Enum columns stored as `VARCHAR`. Never as database-native enum types (portability).  
- JSON fields stored as `TEXT` in both databases (not `JSONB`) for portability.  
- The JVM timezone is forced to UTC at startup via `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))`.

---

## Business Rules

The sections below describe the detailed business rules and processing flows. They are written as implementation specifications — precise enough for an AI agent or developer to implement each feature without ambiguity.

---

## 1\. General Conventions

### 1.1 Timestamps

- All timestamps stored in the database are **UTC**.  
- The application forces `UTC` as the JVM default timezone at startup.  
- Hibernate is configured with `hibernate.jdbc.time_zone=UTC`.  
- `notification_time` in `charge_plan` is a `TIME` value interpreted in `notification_timezone` (IANA format, e.g. `America/Sao_Paulo`). Conversion to UTC happens in the application layer at scheduling time.  
- `CURRENT_TIMESTAMP` / `NOW()` must never be used in business logic queries. The application always passes explicit UTC `Instant` values.

### 1.2 IDs

- All primary keys are **UUID v4** generated by the application before insert.  
- The database never generates IDs.

### 1.3 Monetary Values

- All monetary amounts are stored as `DECIMAL(15,2)`.  
- Rounding uses **HALF\_UP** (standard Brazilian financial rounding).  
- No currency field exists in v1. The system is implicitly single-currency per deployment.

### 1.4 Soft Deletes

- No hard deletes exist in the system.  
- `debtor.is_active = false` deactivates a debtor.  
- `charge_plan_member.status = LEFT` \+ `left_at` removes a member from a plan.  
- `charge_plan.status = FINISHED` or `PAUSED` stops plan processing.  
- Historical data (invoices, payments, logs) is never deleted.

### 1.5 Enum Values

All enum columns use `VARCHAR`. Valid values per column:

| Column | Valid Values |
| :---- | :---- |
| `charge_plan.type` | `ROTATING`, `SPLIT` |
| `charge_plan.status` | `ACTIVE`, `PAUSED`, `FINISHED` |
| `charge_plan.cycle_unit` | `DAILY`, `WEEKLY`, `MONTHLY` |
| `charge_plan_member.status` | `ACTIVE`, `LEFT` |
| `invoice.status` | `PENDING`, `SENT`, `OVERDUE`, `PARTIALLY_PAID`, `PAID`, `CANCELLED` |
| `payment.method` | `PIX`, `BANK_TRANSFER`, `CASH`, `VENMO`, `OTHER` |
| `payment_proof.status` | `PENDING_ANALYSIS`, `PENDING_MANUAL_REVIEW`, `APPROVED`, `REJECTED` |
| `notification_outbox.status` | `SCHEDULED`, `PROCESSING`, `FAILED` |
| `notification_log.status` | `SENT`, `FAILED` |
| `notification_outbox.trigger_type` | `INVOICE_CREATED`, `PENDING_REMINDER`, `OVERDUE_REMINDER`, `PAYMENT_APPROVED`, `PAYMENT_REJECTED` |
| `notification_outbox.channel` | `EMAIL` |

---

## 2\. Debtor Management

### 2.1 Create Debtor

**Endpoint:** `POST /debtors`

**Rules:**

- `name` is required and cannot be blank.  
- `email`, `phone`, and `telegram_handle` are all optional, but at least one contact field should be present if `notifications_enabled = true`. The system does not enforce this as a hard constraint — it is a soft validation with a warning in the response.  
- `email` must be a valid email format if provided.  
- `notifications_enabled` defaults to `true`.  
- `is_active` defaults to `true`.  
- `created_at` and `updated_at` are set to the current UTC instant by the application.  
- A debtor with the same `email` may be created — the system does not enforce email uniqueness. The admin is responsible for deduplication.

### 2.2 Update Debtor

**Endpoint:** `PATCH /debtors/{debtorId}`

**Rules:**

- All fields are optional in the request body. Only provided fields are updated (partial update).  
- `updated_at` is always refreshed on any update.  
- Changing `notifications_enabled` to `false` does not cancel any existing outbox entries. It takes effect on the next scheduling run.  
- Changing `email` does not update existing outbox entries (payload was snapshotted at enqueue time).

### 2.3 Deactivate Debtor

**Endpoint:** `PATCH /debtors/{debtorId}` with `{ "isActive": false }`

**Rules:**

- Setting `is_active = false` hides the debtor from the plan membership assignment UI.  
- Does **not** affect existing `charge_plan_member` records — memberships remain `ACTIVE`.  
- Does **not** cancel any pending invoices.  
- Does **not** affect notification dispatch — `charge_plan_member` entries and their linked invoices continue to be processed normally.  
- To fully remove a debtor from a plan, the admin must separately set `charge_plan_member.status = LEFT`.

### 2.4 List Debtors

**Rules:**

- Default listing returns only `is_active = true` debtors.  
- An `includeInactive=true` query parameter returns all debtors.

---

## 3\. Charge Plan Management

### 3.1 Create Charge Plan

**Endpoint:** `POST /charge-plans`

**Rules:**

- `name`, `type`, `total_amount`, `cycle_interval`, `cycle_unit`, `cycle_anchor_date`, `notification_time`, `notification_timezone`, and `starts_at` are required.  
- `total_amount` must be greater than zero.  
- `cycle_interval` must be a positive integer (\>= 1).  
- `cycle_unit` must be one of `DAILY`, `WEEKLY`, `MONTHLY`.  
- `notification_timezone` must be a valid IANA timezone identifier.  
- `status` is always set to `ACTIVE` on creation regardless of what is sent in the request.  
- `ends_at` and `end_when_recovered` are both optional and independent. Both can be set simultaneously — the plan finishes when whichever condition is met first.  
- `end_when_recovered` must be greater than zero if provided.  
- `ends_at` must be in the future relative to `starts_at`.  
- A newly created plan has no members. Members must be added via `POST /charge-plans/{planId}/members`.  
- The plan is not processed by the scheduler until it has at least one `ACTIVE` member.

### 3.2 Update Charge Plan

**Endpoint:** `PATCH /charge-plans/{planId}`

**Rules:**

- Only `name`, `description`, `notification_time`, `notification_timezone`, `notifications_enabled`, `ends_at`, `end_when_recovered`, `cycle_interval`, `cycle_unit`, and `cycle_anchor_date` can be updated after creation.  
- `type` and `total_amount` **cannot** be changed after the plan has generated at least one invoice. This prevents inconsistency between historical and future invoices.  
- `status` cannot be set directly via this endpoint. Use the dedicated status transition endpoints.

### 3.3 Pause Charge Plan

**Endpoint:** `POST /charge-plans/{planId}/pause`

**Rules:**

- Only `ACTIVE` plans can be paused.  
- Sets `status = PAUSED`.  
- The scheduler immediately stops processing the plan on the next run.  
- Existing pending invoices and outbox entries are **not** affected — they continue to be processed by Huginn.  
- To stop notifications on existing invoices, the admin must cancel them manually.

### 3.4 Resume Charge Plan

**Endpoint:** `POST /charge-plans/{planId}/resume`

**Rules:**

- Only `PAUSED` plans can be resumed.  
- Sets `status = ACTIVE`.  
- The scheduler resumes processing on the next run.  
- Does **not** generate back-fill invoices for cycles that were skipped while paused.

### 3.5 Notification Config

**Endpoint:** `PUT /charge-plans/{planId}/notification-config`

**Rules:**

- Accepts a list of up to 3 config entries, one per `trigger_type`.  
- Each entry is upserted — if a config for that `trigger_type` already exists, it is updated; otherwise it is created.  
- To disable a trigger type, omit it from the list or send `{ "enabled": false }`. The row is deleted from `charge_plan_notification_config`.  
- `reminder_interval` and `reminder_unit` are required for `PENDING_REMINDER` and `OVERDUE_REMINDER`. They are ignored for `INVOICE_CREATED`.  
- `max_attempts` defaults to `5` if not provided.

---

## 4\. Charge Plan Member Management

### 4.1 Add Member to Plan

**Endpoint:** `POST /charge-plans/{planId}/members`

**Rules:**

- `debtor_id` is required.  
- The same debtor cannot be added to the same plan twice if they already have an `ACTIVE` membership. If the debtor has a `LEFT` membership, a new membership record is created (the old one is preserved for history).  
- `amount_override` is optional. If not provided, the plan default is used.  
- For `ROTATING` plans, `rotation_order` is required. The application validates that the provided value does not duplicate an existing `rotation_order` in the plan among `ACTIVE` members.  
- For `SPLIT` plans, `rotation_order` is ignored and stored as `null`.  
- `status` is always `ACTIVE` on creation.  
- `credit_balance` is always `0.00` on creation.  
- `joined_at` is set to the current UTC instant.

### 4.2 Reorder Members (ROTATING plans only)

**Endpoint:** `PUT /charge-plans/{planId}/members/reorder`

**Rules:**

- Only applicable to `ROTATING` plans. Returns `400` for `SPLIT` plans.  
- Request body is an ordered list of `{ memberId, rotationOrder }`.  
- All `ACTIVE` members of the plan must be present in the list. Missing members return `400`.  
- `rotation_order` values must be sequential integers starting at 1 with no gaps (e.g. 1,2,3,4,5). Gaps return `400`.  
- All updates are applied in a single transaction.  
- Takes effect on the next invoice generation cycle. Does not affect already-generated invoices.

### 4.3 Remove Member from Plan (Member Leaves)

**Endpoint:** `PATCH /charge-plans/{planId}/members/{memberId}`

{

  "status": "LEFT",

  "leftAt": "2025-05-01T00:00:00Z",

  "cancelPendingInvoices": true,

  "cancellationReason": "Left the YouTube plan"

}

**Rules:**

- Sets `status = LEFT` and `left_at = leftAt` on the membership record.  
- The member is immediately excluded from future invoice generation and rotation calculation.  
- `leftAt` defaults to the current UTC instant if not provided.  
- If `cancelPendingInvoices = true`:  
  - All invoices for this member with status in `(PENDING, SENT, OVERDUE, PARTIALLY_PAID)` are set to `CANCELLED`.  
  - `cancellation_reason` and `cancelled_at` (current UTC instant) are set on each cancelled invoice.  
  - All `SCHEDULED` outbox entries for those invoices are set to `CANCELLED` (a new status added to the outbox for this purpose, not dispatched by Huginn).  
  - All of the above happens in a single transaction.  
- If `cancelPendingInvoices = false` (default):  
  - Existing invoices are untouched. The member remains billable for open invoices.  
- After the member is set to `LEFT`, the admin should call the reorder endpoint to update `rotation_order` for remaining members if desired.

### 4.4 Credit Balance (Manual Pre-payment)

**Endpoint:** `POST /charge-plans/{planId}/members/{memberId}/credit`

{

  "amount": 50.00,

  "notes": "Paid in advance via bank transfer"

}

**Rules:**

- `amount` must be greater than zero.  
- Adds the amount to `charge_plan_member.credit_balance`.  
- Does **not** create a `Payment` record directly — credit is consumed when the next invoice is generated (see Invoice Generation rules).  
- The `notes` field is stored as a `Payment` record with `approved_manually = true` and `payment_proof_id = null`, linked to the next generated invoice. Actually: since no invoice exists yet, it is stored as a standalone admin note outside the payment table — implementation detail left to the backend team.

---

## 5\. Invoice Generation

Executed by **Odin**, which runs daily at `00:00 UTC`.

### 5.1 Determining Whether to Generate Invoices

For each `charge_plan` with `status = ACTIVE`:

1. Calculate the **next generation date** using:  
     
   next\_date \= cycle\_anchor\_date \+ (N \* cycle\_interval \* cycle\_unit)  
     
   where N \= smallest integer \>= 0 such that the resulting date \>= today  
     
2. If `next_date == today (UTC date)` → proceed to generation.  
3. If `next_date != today` → skip this plan.

**Edge case — MONTHLY with anchor on day 31:** If `cycle_anchor_date` is the 31st and the current month has fewer days, generation occurs on the last day of that month (e.g. February 28/29).

### 5.2 Generation for ROTATING Plans

1. Count `total_active_members` \= number of `charge_plan_member` records with `status = ACTIVE` for this plan.  
2. If `total_active_members = 0` → skip, log a warning.  
3. Determine the current `cycle_index`:  
   - Query the maximum `cycle_index` from existing invoices for this plan.  
   - If no invoices exist yet: `cycle_index = 0`.  
   - Otherwise: `cycle_index = max_cycle_index + 1`.  
4. Determine the responsible member:  
     
   position \= cycle\_index % total\_active\_members  
     
   responsible \= ACTIVE members ordered by rotation\_order ASC \[ position \]  
     
5. Calculate `amount_due`:  
   - If `responsible.amount_override` is not null: `amount_due = amount_override`  
   - Otherwise: `amount_due = charge_plan.total_amount`  
   - Deduct `credit_balance`: `amount_due = MAX(0, amount_due - credit_balance)`  
   - If `credit_balance > amount_due` (credit exceeds due): `amount_due = 0`, carry remaining credit forward.  
   - Update `charge_plan_member.credit_balance = MAX(0, credit_balance - original_amount_due)`.  
6. Create one `invoice` record:  
   - `charge_plan_id` \= plan id  
   - `charge_plan_member_id` \= responsible member id  
   - `cycle_index` \= calculated index  
   - `amount_due` \= calculated amount  
   - `amount_paid` \= `0.00`  
   - `status` \= `PENDING`  
   - `due_date` \= today \+ `due_tolerance_days` (configurable, default 5 days, stored in `charge_plan` — **add this field**)  
   - `created_at` \= `updated_at` \= current UTC instant  
7. If `amount_due = 0` (fully covered by credit): set `status = PAID`, `paid_at = created_at`. Do **not** enqueue a notification. Do **not** proceed to notification scheduling for this invoice.

### 5.3 Generation for SPLIT Plans

1. Fetch all `charge_plan_member` records with `status = ACTIVE` for this plan.  
2. If no active members → skip, log a warning.  
3. Determine the current `cycle_index` (same logic as ROTATING).  
4. For each active member:  
   - Calculate `amount_due`:  
     - If `member.amount_override` is not null: `amount_due = amount_override`  
     - Otherwise: `amount_due = charge_plan.total_amount / total_active_members` (rounded HALF\_UP)  
   - Apply `credit_balance` deduction (same logic as ROTATING step 5).  
   - Create one `invoice` record per member.  
   - If `amount_due = 0`: mark as `PAID` immediately, skip notification.  
5. **Rounding correction:** the sum of all member `amount_due` values may differ from `total_amount` by at most `0.01` due to rounding. Apply the rounding correction to the first member in the list (lowest `rotation_order` or insertion order).

### 5.4 Post-Generation: Enqueue INVOICE\_CREATED Notification

After invoices are created (only for invoices with `status = PENDING`):

1. Check if `charge_plan.notifications_enabled = true`.  
2. Check if a `charge_plan_notification_config` row exists for this plan with `trigger_type = INVOICE_CREATED`.  
3. Check if `debtor.notifications_enabled = true` (via member → debtor join).  
4. Check if `debtor.email` is not null.  
5. If all conditions are met:  
   - Generate a JWT upload token (signed, expires in 48 hours from `notification_time` today).  
   - Calculate `scheduled_for`:  
       
     ZonedDateTime scheduledLocal \= LocalDate.now(planZone).atTime(notificationTime).atZone(planZone);  
       
     Instant scheduledUtc \= scheduledLocal.toInstant();  
       
   - Create `notification_outbox` entry with:  
     - `trigger_type = INVOICE_CREATED`  
     - `status = SCHEDULED`  
     - `attempt_count = 0`  
     - `max_attempts` \= value from `charge_plan_notification_config`  
     - `payload` \= JSON snapshot: `{ debtorName, planName, amountDue, dueDate, uploadLink, cycleIndex }`  
     - `scheduled_for` \= calculated UTC instant  
6. All inserts (invoice \+ outbox) happen in a **single transaction**.

---

## 6\. Invoice Lifecycle

### 6.1 Overdue Detection

Executed by **Odin**, daily.

For each `invoice` with `status IN (SENT, PARTIALLY_PAID)`:

1. If `due_date < current UTC instant`:  
   - Set `status = OVERDUE`.  
   - Set `updated_at` \= current UTC instant.  
2. Check if a `SCHEDULED` outbox entry for `trigger_type = OVERDUE_REMINDER` already exists for this invoice.  
3. If not: create one (see [Notification Scheduling](#10-notification-scheduling-job-a)).

### 6.2 Pending Reminder Scheduling

Executed by **Odin**, daily.

For each `invoice` with `status = SENT` and `due_date >= current UTC date`:

1. Check if a `charge_plan_notification_config` row exists for `trigger_type = PENDING_REMINDER`.  
2. If not: skip.  
3. Query `notification_log` for the most recent `status = SENT` entry for this invoice with `trigger_type = PENDING_REMINDER`.  
4. If no prior log exists: schedule immediately (enqueue for today's `notification_time`).  
5. If a prior log exists:  
     
   next\_send \= last\_sent\_at \+ (reminder\_interval \* reminder\_unit)  
     
   If `next_send <= today`: enqueue for today's `notification_time`.  
     
6. Before enqueuing, verify no `SCHEDULED` entry already exists for this invoice \+ `PENDING_REMINDER` (idempotency guard).

### 6.3 Status Transition Rules

| From | To | Trigger |
| :---- | :---- | :---- |
| `PENDING` | `SENT` | Huginn successfully dispatches `INVOICE_CREATED` notification |
| `SENT` | `OVERDUE` | Odin detects `due_date` passed |
| `SENT` | `PARTIALLY_PAID` | Payment approved, `amount_paid < amount_due` |
| `SENT` | `PAID` | Payment approved, `amount_paid >= amount_due` |
| `OVERDUE` | `PARTIALLY_PAID` | Payment approved, `amount_paid < amount_due` |
| `OVERDUE` | `PAID` | Payment approved, `amount_paid >= amount_due` |
| `PARTIALLY_PAID` | `OVERDUE` | Odin detects `due_date` passed |
| `PARTIALLY_PAID` | `PAID` | Payment approved, `amount_paid >= amount_due` |
| `PENDING` | `CANCELLED` | Admin cancels, or member leaves with `cancelPendingInvoices=true` |
| `SENT` | `CANCELLED` | Admin cancels, or member leaves with `cancelPendingInvoices=true` |
| `OVERDUE` | `CANCELLED` | Admin cancels, or member leaves with `cancelPendingInvoices=true` |

**Terminal states:** `PAID`, `CANCELLED`. No transitions out.

---

## 7\. Payment Proof — Upload Flow

### 7.1 Token Validation

**Endpoint:** `POST /public/invoices/{invoiceId}/proof` (unauthenticated, token in header or query param)

**Rules:**

1. Extract JWT token from the request.  
2. Verify JWT signature using the application's secret key.  
3. Verify JWT has not expired (`exp` claim).  
4. Verify JWT `invoiceId` claim matches the `{invoiceId}` in the URL.  
5. Verify a `payment_proof` record does **not** already exist for this invoice with `status NOT IN (REJECTED)`. If an active proof already exists, return `409 Conflict` — a proof is already pending or approved.  
6. Verify the invoice `status` is not `PAID` or `CANCELLED`. If so, return `409 Conflict`.  
7. If all checks pass: accept the file upload.

### 7.2 File Storage

**Rules:**

- Accepted file types: `image/jpeg`, `image/png`, `image/webp`, `application/pdf`.  
- Maximum file size: `10MB` (configurable via application property).  
- File is saved to the configured storage directory with a UUID-based filename to avoid collisions.  
- `file_path` stores the relative path from the storage root.

### 7.3 Proof Record Creation

After file is saved:

1. Determine `proof_validation_mode` from `charge_plan` (via invoice → member → plan).  
2. Create `payment_proof` record:  
   - `status`:  
     - `AI_AUTO` or `AI_ASSISTED` → `PENDING_ANALYSIS`  
     - `MANUAL` → `PENDING_MANUAL_REVIEW`  
   - `upload_token` \= the token used (for audit)  
   - `token_expires_at` \= the token's `exp` claim  
   - `requires_manual_review = false`  
   - All value fields (`ai_extracted_value`, `final_value`) \= `null`  
3. If mode is `AI_AUTO` or `AI_ASSISTED`: trigger async AI analysis (see [Section 8](#8-payment-proof--ai-analysis-flow)) in a background thread.  
4. Return `202 Accepted` immediately with a message indicating the proof is under analysis. The debtor does not wait for the result.

**Response body (202):**

{

  "proofId": "uuid",

  "message": "Your payment proof has been received and is being reviewed. You will be notified of the result."

}

---

## 8\. Payment Proof — Validation Flow

### 8.1 Validation Modes

The `charge_plan.proof_validation_mode` field controls how each proof is processed:

| Mode | Description |
| :---- | :---- |
| `AI_AUTO` | AI analyzes the proof and makes the final decision automatically. No human review unless AI fails. |
| `MANUAL` | No AI involved. Proof goes directly to the admin review queue. |
| `AI_ASSISTED` | AI analyzes the proof. If valid (`isValid = true`), routes to admin review queue for final confirmation. If invalid, rejects automatically. |

### 8.2 MANUAL Flow

Triggered immediately when proof is created with `proof_validation_mode = MANUAL`.

1. `payment_proof.status` is set to `PENDING_MANUAL_REVIEW` at creation time (Section 7.3).  
2. Proof surfaces in the admin review queue.  
3. Admin resolves via Section 9.3.

### 8.3 AI Analysis (AI\_AUTO and AI\_ASSISTED)

**Triggered:** asynchronously in a background thread after proof upload.

**Rules:**

1. Load the file from storage.  
2. Send to the configured AI provider (Anthropic API) with the following prompt contract:  
   - Input: the proof file (image or PDF) \+ invoice context (amount due, debtor name, plan name).  
   - Expected JSON output:  
       
     {  
       
       "isValid": true,  
       
       "valueReceived": 83.00,  
       
       "paymentDate": "2025-05-01",  
       
       "receiverName": "Mateus Silva",  
       
       "method": "PIX",  
       
       "rejectionReason": null  
       
     }

     
3. Store the full raw response in `payment_proof.ai_raw_response`.

### 8.4 AI\_AUTO — Post-Analysis Routing

After AI analysis completes:

- If `isValid = true`:  
  - Set `ai_extracted_value = valueReceived`.  
  - Set `final_value = valueReceived`.  
  - Set `status = APPROVED`.  
  - Trigger payment registration (Section 9.1).  
  - Enqueue `PAYMENT_APPROVED` notification (Section 9.4).  
- If `isValid = false`:  
  - Set `status = REJECTED`.  
  - Set `requires_manual_review = false`.  
  - Enqueue `PAYMENT_REJECTED` notification (Section 9.4).

### 8.5 AI\_ASSISTED — Post-Analysis Routing

After AI analysis completes:

- If `isValid = true`:  
  - Set `ai_extracted_value = valueReceived`.  
  - Set `final_value = valueReceived`.  
  - Set `requires_manual_review = true`.  
  - Set `status = PENDING_MANUAL_REVIEW`.  
  - Proof surfaces in the admin review queue. No payment created yet.  
- If `isValid = false`:  
  - Set `status = REJECTED`.  
  - Set `requires_manual_review = false`.  
  - Enqueue `PAYMENT_REJECTED` notification (Section 9.4).

### 8.6 AI Analysis Failure Handling

If the AI API call fails (timeout, error response, unparseable JSON) for `AI_AUTO` or `AI_ASSISTED`:

1. Log the error.  
2. Set `payment_proof.status = PENDING_MANUAL_REVIEW`.  
3. Set `requires_manual_review = true`.  
4. Proof surfaces in the admin review queue with an error indicator.  
5. Admin resolves manually via Section 9.3.  
6. No notification is sent to the debtor at this point — they will be notified only when the admin makes a final decision.

---

## 9\. Payment Registration

### 9.1 Automatic Payment (from Approved Proof)

**Triggered:** when `payment_proof.status` transitions to `APPROVED` (via AI\_AUTO or admin approval).

**Rules** (all in a single transaction):

1. Create `payment` record:  
   - `invoice_id` \= the proof's `invoice_id`  
   - `payment_proof_id` \= the proof's id  
   - `amount` \= `payment_proof.final_value`  
   - `method` \= value extracted by AI if available, otherwise `OTHER`  
   - `approved_manually = false`  
   - `paid_at` \= `paymentDate` returned by AI (parsed to UTC instant), or current UTC instant if not available  
2. Update `invoice.amount_paid`:  
     
   new\_amount\_paid \= current amount\_paid \+ payment.amount  
     
3. Transition invoice status:  
   - If `new_amount_paid >= amount_due`: set `status = PAID`, `paid_at = now()`.  
   - If `new_amount_paid < amount_due`: set `status = PARTIALLY_PAID` (or keep `OVERDUE` if already overdue).  
4. If invoice is now `PAID`: cancel all `SCHEDULED` outbox reminder entries for this invoice (no more reminders needed).  
5. If `PARTIALLY_PAID`: leave reminders active.

### 9.2 Manual Payment (Admin)

**Endpoint:** `POST /invoices/{invoiceId}/payments`

{

  "amount": 83.00,

  "method": "PIX",

  "paidAt": "2025-05-01T14:30:00Z",

  "notes": "Confirmed via WhatsApp screenshot"

}

**Rules:**

- `amount` must be greater than zero.  
- `amount` can exceed `amount_due - amount_paid` (overpayment). Excess is stored but invoice is marked `PAID`. Overpayment is **not** automatically converted to `credit_balance` in v1.  
- `paidAt` defaults to current UTC instant if not provided.  
- `approved_manually = true`, `payment_proof_id = null`.  
- Same invoice status transition logic as Section 9.1.  
- Does **not** enqueue `PAYMENT_APPROVED` notification — manual payments are admin-initiated and the debtor is assumed to already be aware.

### 9.3 Manual Review Resolution (Admin)

**Endpoint:** `POST /admin/proofs/{proofId}/resolve`

{

  "decision": "APPROVED",

  "finalValue": 83.00

}

**Rules:**

- `decision` must be `APPROVED` or `REJECTED`.  
- Only proofs with `status = PENDING_MANUAL_REVIEW` can be resolved.  
- If `APPROVED`:  
  - Set `final_value = finalValue`.  
  - Set `status = APPROVED`.  
  - Trigger automatic payment registration (Section 9.1).  
  - Enqueue `PAYMENT_APPROVED` notification (Section 9.4).  
- If `REJECTED`:  
  - Set `status = REJECTED`.  
  - No payment created.  
  - Enqueue `PAYMENT_REJECTED` notification (Section 9.4).

### 9.4 Payment Result Notification

Triggered when a proof transitions to `APPROVED` or `REJECTED` via any path (AI\_AUTO, AI\_ASSISTED, or admin resolution).

**Rules:**

1. Determine `trigger_type`:  
   - `status = APPROVED` → `trigger_type = PAYMENT_APPROVED`  
   - `status = REJECTED` → `trigger_type = PAYMENT_REJECTED`  
2. Check `charge_plan_notification_config` for a row matching `trigger_type`.  
   - If no row exists or it is disabled → do not notify.  
3. Check `charge_plan.notifications_enabled = true` AND `debtor.notifications_enabled = true`.  
4. Check `debtor.email` is not null.  
5. If all conditions pass: insert into `notification_outbox`:  
   - `scheduled_for = NOW()` (dispatched on Huginn's next run, within 1 minute)  
   - `status = SCHEDULED`  
   - `payload` snapshot includes: debtor name, plan name, invoice amount, result (approved/rejected), rejection reason if applicable.

**These notifications are never scheduled by Odin.** They are enqueued inline, at the moment the proof status transitions, within the same transaction.

**Default config on ChargePlan creation:** When a new `ChargePlan` is created, the system automatically inserts `charge_plan_notification_config` rows for `PAYMENT_APPROVED` and `PAYMENT_REJECTED` with notifications enabled. Admin can disable them per plan if desired.

## 10\. Notification Scheduling — Odin

Odin runs daily at `00:00 UTC`. It only **schedules** (writes to `notification_outbox`). It never sends. Odin does **not** consult `notification_log` to decide whether to enqueue reminders. All reminder scheduling is calculated purely from `invoice.created_at`, `invoice.due_date`, and the `reminder_interval` \+ `reminder_unit` from `charge_plan_notification_config`. `notification_log` is exclusively an audit trail — no business logic depends on it.

### 10.1 INVOICE\_CREATED

Scheduled during invoice generation (see [Section 5.4](#54-post-generation-enqueue-invoice_created-notification)).

### 10.2 PENDING\_REMINDER

Triggered for invoices with `status = SENT` and `due_date >= today` (not yet overdue).

**Scheduling logic:**

- Anchor date: `invoice.created_at`  
- For each `N = 1, 2, 3...`:  
    
  candidate\_date \= invoice.created\_at \+ (N \* reminder\_interval \* reminder\_unit)  
    
  If `candidate_date (date part only) == today (UTC)` → enqueue. Stop loop.

**Example:** invoice created on June 1, `reminder_interval = 3`, `reminder_unit = DAILY`. Reminders are scheduled for June 4, June 7, June 10, etc. Odin enqueues on each of those days if the invoice is still `SENT`.

### 10.3 OVERDUE\_REMINDER

Triggered for invoices with `status = OVERDUE` or `status = PARTIALLY_PAID` where `due_date < today`.

**Scheduling logic:**

- Anchor date: `invoice.due_date`  
- For each `N = 1, 2, 3...`:  
    
  candidate\_date \= invoice.due\_date \+ (N \* reminder\_interval \* reminder\_unit)  
    
  If `candidate_date (date part only) == today (UTC)` → enqueue. Stop loop.

**Example:** invoice due on June 5, `reminder_interval = 2`, `reminder_unit = DAILY`. Overdue reminders are scheduled for June 7, June 9, June 11, etc.

### 10.4 Idempotency Guard (Odin)

Before enqueuing **any** outbox entry (all trigger types), Odin checks:

SELECT COUNT(\*) FROM notification\_outbox

WHERE invoice\_id \= :invoiceId

  AND trigger\_type \= :triggerType

  AND status IN ('SCHEDULED', 'PROCESSING')

If count \> 0 → **do not enqueue**. A notification for this invoice and trigger type is already in flight.

This prevents duplicates when Odin runs on a day that matches a reminder date but Huginn has not yet dispatched the previous entry (e.g. SMTP is down).

### 10.5 Enqueue Rules (all trigger types)

Before enqueuing, always verify all of the following:

1. `charge_plan.notifications_enabled = true`  
2. `debtor.notifications_enabled = true`  
3. `debtor.email` is not null  
4. A `charge_plan_notification_config` row exists for the `trigger_type`  
5. Idempotency guard passes (Section 10.4)

If all pass, create `notification_outbox` entry:

- `status = SCHEDULED`  
- `attempt_count = 0`  
- `max_attempts` \= value from `charge_plan_notification_config`  
- `scheduled_for` \= today's date \+ `charge_plan.notification_time` converted to UTC  
- `payload` \= JSON snapshot (see Section 10.6)

All outbox inserts happen in the **same transaction** as the operation that triggered them (invoice generation, overdue detection).

### 10.6 Notification Payload Snapshot

The `payload` JSON stored in `notification_outbox` must contain all data needed for the email template, captured at enqueue time. Huginn must be able to send the notification without any additional database queries.

{

  "debtorName": "Gustavo Ferreira",

  "debtorEmail": "gustavo@example.com",

  "planName": "YouTube Premium",

  "amountDue": 27.50,

  "amountPaid": 0.00,

  "dueDate": "2025-06-10T00:00:00Z",

  "cycleIndex": 4,

  "uploadLink": "https://caiman.local/public/invoices/{id}/proof?token=eyJ...",

  "triggerType": "INVOICE\_CREATED"

}

The upload link embeds a JWT token generated at enqueue time. Token expires 48 hours after `scheduled_for`.

---

## 11\. Notification Dispatch — Huginn

Huginn runs **every minute**. It only dispatches. It never generates invoices or changes invoice status.

**The `notification_outbox` is a work queue, not a history table.** Huginn **deletes** rows from the outbox when they reach a terminal state (sent or exhausted). All history is preserved in `notification_log` (append-only, never deleted). This keeps the outbox small and fast regardless of how long the system runs.

### 11.1 Dispatch Loop

1\. SELECT \* FROM notification\_outbox

   WHERE status \= 'SCHEDULED'

     AND scheduled\_for \<= NOW() (UTC)

   ORDER BY scheduled\_for ASC

   LIMIT 50  \<- batch size, configurable

2\. For each entry:

   a. UPDATE status \= 'PROCESSING'

      (skip if already PROCESSING — concurrency guard)

   b. Attempt to send via the configured channel (EMAIL in v1)

   c. On SUCCESS:

      \- INSERT notification\_log (status \= SENT, sent\_at \= NOW())

      \- DELETE FROM notification\_outbox WHERE id \= :entryId

      \- If trigger\_type \= INVOICE\_CREATED AND invoice.status \= PENDING:

          UPDATE invoice SET status \= 'SENT'

   d. On FAILURE:

      \- INSERT notification\_log (status \= FAILED, error\_message \= exception)

      \- INCREMENT attempt\_count

      \- If attempt\_count \>= max\_attempts:

          DELETE FROM notification\_outbox WHERE id \= :entryId

          \<- entry is gone; the FAILED log entries remain as audit trail

        Else:

          next\_retry \= NOW() \+ (2 ^ attempt\_count) minutes (capped at 60 min)

          UPDATE notification\_outbox SET

            status \= 'SCHEDULED',

            scheduled\_for \= next\_retry,

            last\_attempted\_at \= NOW(),

            last\_error \= :errorMessage

### 11.2 Valid Outbox Statuses

With the delete-on-terminal-state approach, only three statuses exist in the outbox at any given time:

| Status | Meaning |
| :---- | :---- |
| `SCHEDULED` | Waiting to be dispatched. `scheduled_for` not yet reached, or rescheduled after a failed attempt. |
| `PROCESSING` | Currently being dispatched by Huginn. Prevents double-dispatch. |
| `FAILED` | Last attempt failed. Will be retried. `attempt_count < max_attempts`. |

`SENT` and `EXHAUSTED` never persist in the outbox — the row is deleted before those states would be set.

### 11.3 Invoice Status Transition on INVOICE\_CREATED Dispatch

- When Huginn successfully sends an `INVOICE_CREATED` notification:  
  - If the linked invoice has `status = PENDING` → set `status = SENT`.  
  - If the invoice is already in any other state → do not change it.

### 11.4 Idempotency — Stuck PROCESSING Entries

- If Huginn crashes mid-dispatch, an entry can be stuck in `PROCESSING` indefinitely.  
- On each Huginn run, before the main dispatch loop:  
    
  UPDATE notification\_outbox  
    
  SET status \= 'SCHEDULED',  
    
      last\_error \= 'Reset from stuck PROCESSING state',  
    
      attempt\_count \= attempt\_count \+ 1  
    
  WHERE status \= 'PROCESSING'  
    
    AND last\_attempted\_at \< NOW() \- INTERVAL '5 minutes'  
    
- If `attempt_count` after increment \>= `max_attempts`:  
  - Insert a `FAILED` entry into `notification_log`.  
  - Delete the outbox row.

### 11.5 Email Dispatch Rules

- Email is sent using the configured SMTP settings (application properties).  
- Subject and body are rendered from templates (implementation detail, not a business rule).  
- If `debtor.email` is null in the payload (should not happen — Odin validates before enqueue, but payload could be stale):  
  - Do not attempt to send.  
  - Log as `FAILED` with reason `"Recipient email missing in payload"`.  
  - Delete the outbox entry immediately (do not retry — this is a config error, not a transient failure).

## 12\. Admin Operations

### 12.1 Resend Notification Link

**Endpoint:** `POST /admin/invoices/{invoiceId}/resend-link`

**Rules:**

1. Invoice must not be `PAID` or `CANCELLED`.  
2. Generate a new JWT upload token (48h expiry from now).  
3. Create a new `notification_outbox` entry with:  
   - `trigger_type = INVOICE_CREATED`  
   - `scheduled_for = NOW()` (send immediately on next Huginn run)  
   - Fresh payload snapshot with the new token.  
4. Log as `LINK_RESENT` in `notification_log` (add this `trigger_type` value).  
5. Does **not** change the invoice status.

### 12.2 Cancel Invoice (Admin)

**Endpoint:** `POST /admin/invoices/{invoiceId}/cancel`

{

  "reason": "Member left the plan"

}

**Rules:**

1. Only invoices with `status IN (PENDING, SENT, OVERDUE)` can be cancelled. `PARTIALLY_PAID` requires explicit confirmation flag: `{ "reason": "...", "confirmPartialCancel": true }`.  
2. Set `status = CANCELLED`, `cancelled_at = NOW()`, `cancellation_reason = reason`.  
3. Cancel all `SCHEDULED` outbox entries for this invoice.  
4. Existing `PAYMENT` records linked to this invoice are **not** deleted (audit trail).

### 12.3 Admin Manual Proof Review Queue

**Endpoint:** `GET /admin/proofs/pending-review`

Returns all `payment_proof` records with `status = PENDING_MANUAL_REVIEW`, ordered by `created_at ASC`.

---

## 13\. Charge Plan Termination

### 13.1 Automatic Termination by Date

Checked by **Odin**, daily, after invoice generation.

**Rules:**

1. If `charge_plan.ends_at` is not null AND `ends_at <= current UTC instant`:  
   - Set `status = FINISHED`.  
   - No more invoices are generated.  
   - Existing pending invoices and outbox entries are **not** affected.

### 13.2 Automatic Termination by Recovered Amount

Checked by **Odin**, daily, after invoice generation.

**Rules:**

1. If `charge_plan.end_when_recovered` is not null:  
   - Calculate `total_recovered`:  
       
     SELECT COALESCE(SUM(p.amount), 0\)  
       
     FROM payment p  
       
     JOIN invoice i ON i.id \= p.invoice\_id  
       
     WHERE i.charge\_plan\_id \= :planId  
       
   - If `total_recovered >= end_when_recovered`:  
     - Set `status = FINISHED`.

### 13.3 Manual Termination

**Endpoint:** `POST /charge-plans/{planId}/finish`

**Rules:**

- Any `ACTIVE` or `PAUSED` plan can be manually finished.  
- Sets `status = FINISHED`.  
- Does **not** cancel pending invoices.

---

## 14\. Validation Rules Summary

| Entity | Field | Rule |
| :---- | :---- | :---- |
| `debtor` | `name` | Required, non-blank, max 255 chars |
| `debtor` | `email` | Valid email format if provided |
| `charge_plan` | `total_amount` | Required, \> 0 |
| `charge_plan` | `cycle_interval` | Required, \>= 1 |
| `charge_plan` | `notification_timezone` | Valid IANA timezone |
| `charge_plan` | `ends_at` | Must be after `starts_at` if provided |
| `charge_plan` | `end_when_recovered` | Must be \> 0 if provided |
| `charge_plan` | `type` \+ `total_amount` | Cannot change after first invoice is generated |
| `charge_plan_member` | `rotation_order` | Required for ROTATING, no duplicates within plan |
| `charge_plan_member` | `amount_override` | Must be \> 0 if provided |
| `charge_plan_member` | `credit_balance` | Cannot be negative |
| `invoice` | `amount_due` | Must be \>= 0 (can be 0 if fully covered by credit) |
| `payment` | `amount` | Must be \> 0 |
| `payment_proof` | `userConfirmedValue` | Must be \> 0 |
| `notification_outbox` | `max_attempts` | Must be \>= 1 |
| Reorder endpoint | `rotation_order` list | Sequential from 1, no gaps, all ACTIVE members present |

---

## Appendix A — DDL Corrections Identified During Rule Writing

The following changes to the DDL were identified during the writing of these rules. All items below must be applied before implementation begins.

### A.1 Missing field — `charge_plan.due_tolerance_days`

| Table | Field | Type | Default | Nullable | Purpose |
| :---- | :---- | :---- | :---- | :---- | :---- |
| `charge_plan` | `due_tolerance_days` | `INTEGER` | `5` | `NOT NULL` | Number of days after invoice generation date to set `due_date`. Example: generated on June 1 with tolerance 5 → `due_date = June 6`. |

Add as **V3** migration.

### A.2 Remove `notification_outbox.processed_at`

The field `processed_at` in `notification_outbox` was designed for a "mark as SENT/EXHAUSTED" approach. The final design deletes rows on terminal state instead. `processed_at` is therefore unnecessary.

Remove from the `notification_outbox` table definition in **V1** (before any data exists) or add a **V3** migration to drop the column.

### A.3 Remove `notification_outbox` statuses `SENT` and `EXHAUSTED`

The outbox only holds in-flight entries. Terminal states (`SENT`, `EXHAUSTED`) never persist — the row is deleted. Valid outbox statuses are: `SCHEDULED`, `PROCESSING`, `FAILED` only.

Update the application-layer enum. No DDL change needed (stored as `VARCHAR`).

### A.4 Remove index `idx_log_invoice_trigger_sent`

This index was created to support querying "last successful send time per invoice and trigger type" from `notification_log`. That query no longer exists — Odin calculates reminder dates from `invoice.created_at` and `invoice.due_date` only. The index is unused and should be removed from the DDL.

Remove from **V1** DDL (changeSet `10-create-indexes`).

### A.5 New field — `charge_plan.proof_validation_mode`

| Table | Field | Type | Default | Nullable | Purpose |
| :---- | :---- | :---- | :---- | :---- | :---- |
| `charge_plan` | `proof_validation_mode` | `VARCHAR(50)` | `AI_AUTO` | `NOT NULL` | Controls how payment proofs are validated. One of: `AI_AUTO`, `MANUAL`, `AI_ASSISTED`. |

Add as **V3** migration (alongside `due_tolerance_days`).

### A.6 Remove field — `payment_proof.user_confirmed_value`

The user confirmation step has been removed from the proof upload flow. The debtor no longer sees or confirms the AI-extracted value. The field `user_confirmed_value` is therefore unused.

Remove from `payment_proof` table. Add as a `dropColumn` in **V3** migration.

### A.7 Remove status — `payment_proof.PENDING_USER_CONFIRM`

The status `PENDING_USER_CONFIRM` no longer exists. Valid `payment_proof.status` values are: `PENDING_ANALYSIS`, `PENDING_MANUAL_REVIEW`, `APPROVED`, `REJECTED`.

No DDL change needed (stored as `VARCHAR`). Update application-layer enum only.

### A.8 Default notification config rows on ChargePlan creation

When a new `ChargePlan` is created, the application must automatically insert `charge_plan_notification_config` rows for `PAYMENT_APPROVED` and `PAYMENT_REJECTED` with notifications enabled. This is application logic, not a DDL change.  
