# User Story: Register a ChargePlan and Generate its first Invoice

## Narrative

As the **Admin**, I want to register a **ChargePlan** (with its **ChargePlanMember** list) and have **Odin** automatically generate the corresponding **Invoice** on the correct cycle date, so that the happy path of the billing engine — from plan setup to a real charge being issued — works end to end.

## Context

- `Debtor` registration already exists (`POST /debtors`).
- `ChargePlan` registration already exists (`POST /charge-plans`), including nested `ChargePlanMember` and `ChargePlanNotificationConfig` creation.
- What's missing to close the loop: the `Invoice` domain itself does not exist yet (no model, no persistence), and nothing generates an `Invoice` from a registered `ChargePlan`. That is **Odin**'s job — it does not exist yet either.
- This story covers the full flow, including the already-implemented `ChargePlan` registration, because the story's unit of value is the end-to-end result (plan registered → invoice generated), not an individual endpoint.

## Scope

### In scope

1. **ChargePlan registration** (already done — included here as precondition/context, not to be reimplemented):
   - `POST /charge-plans` creates a `ChargePlan` (`ROTATING` or `SPLIT`), optionally with `members` and `notificationConfigs` in the same request.

2. **Invoice domain foundation** (new):
   - `Invoice` domain model, persistence (entity, repository, gateway/adapter), mirroring the `invoice` table (`charge_plan_id`, `charge_plan_member_id`, `cycle_index`, `amount_due`, `amount_paid`, `status`, `due_date`, `cancellation_reason`, `cancelled_at`, `paid_at`).

3. **Odin — invoice generation** (new):
   - Daily scheduled job that, for each `ACTIVE` `ChargePlan`, determines whether today matches the next generation date derived from `cycle_anchor_date`, `cycle_interval`, and `cycle_unit`.
   - **ROTATING plans:** determines the responsible member from `rotation_order` and the current `cycle_index` (`cycle_index % total_active_members`), applies `amount_override` or falls back to `total_amount`, deducts `credit_balance`, and creates a single `Invoice`.
   - **SPLIT plans:** creates one `Invoice` per `ACTIVE` `ChargePlanMember`, splitting `total_amount` across active members (`amount_override` takes precedence per member), applying the same `credit_balance` deduction logic and rounding correction rule.
   - `due_date` is calculated as generation date + `due_tolerance_days`.
   - If `amount_due` is fully covered by `credit_balance`, the `Invoice` is created directly as `PAID` (no notification path involved).
```text
1. Gera invoices (InvoiceGeneratedEvent)
2. Detecta overdue (InvoiceOverdueDetectedEvent)
3. Agenda reminders (PendingReminderDueEvent)
```


## Out of scope

- Notification enqueueing (`INVOICE_CREATED`, business rule section 5.4) — depends on the `caiman-notification` module, which does not exist yet.
- Overdue detection and `PENDING_REMINDER` scheduling (business rules sections 6.1, 6.2, 10) — separate story, also depends on notifications.
- `Invoice` read endpoints (`GET /invoices`, `GET /invoices/{id}`) — not required to prove the happy path; can be validated via integration test / direct query for now.
- Any `ChargePlan` or `ChargePlanMember` mutation endpoints (update, pause/resume, reorder, leave/reactivate, credit) — explicitly deferred to later stories.
- `PaymentProof` / `Payment` flow — separate bounded context, not touched by this story.

## Acceptance Criteria

- Given an `ACTIVE` `ROTATING` ChargePlan with active members and a `cycle_anchor_date` matching today, when Odin runs, then exactly one `Invoice` is created for the member at `rotation_order` position `cycle_index % total_active_members`, with `amount_due` correctly reflecting `amount_override` and `credit_balance` deduction.
- Given an `ACTIVE` `SPLIT` ChargePlan with active members and a matching cycle date, when Odin runs, then one `Invoice` is created per `ACTIVE` member, with `total_amount` split and rounded `HALF_UP`, and any rounding remainder applied to the first member.
- Given a `ChargePlan` whose next generation date does not match today, when Odin runs, then no `Invoice` is created for that plan.
- Given a `ChargePlan` with zero `ACTIVE` members, when Odin runs, then generation is skipped for that plan and a warning is logged.
- Given a member whose `credit_balance` fully covers the calculated `amount_due`, when the `Invoice` is generated, then its `status` is `PAID` and `credit_balance` is decremented accordingly.
- End-to-end: registering a `ChargePlan` (with members) via `POST /charge-plans` and then running Odin results in the expected `Invoice` row(s) in the database, with correct `status`, `amount_due`, `due_date`, and `cycle_index`.
