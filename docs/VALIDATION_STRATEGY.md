# Validation Strategy

This document defines where and how validation is implemented across the Caiman backend architecture.

---

## The Three-Layer Model

Validation occurs at three distinct layers. Each layer has a different responsibility. This is **not duplication** — it is Separation of Concerns.

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────────────────┐
│  Layer 1: Controller / Entrypoint                   │
│  Jakarta Validation (@Valid, @NotBlank, @Size,      │
│  custom @Constraint)                                │
│  Rejects: malformed input, format errors            │
│  Response: HTTP 400 Bad Request                     │
└──────────────────────────┬──────────────────────────┘
                           │ well-formed Command
                           ▼
┌─────────────────────────────────────────────────────┐
│  Layer 2: UseCase / Service                         │
│  Business rule validation before domain             │
│  object construction                                │
│  Rejects: business rule violations, missing         │
│  resources                                          │
│  Response: HTTP 422 Unprocessable Entity            │
│            HTTP 404 Not Found                       │
└──────────────────────────┬──────────────────────────┘
                           │ valid Command, rules passed
                           ▼
┌─────────────────────────────────────────────────────┐
│  Layer 3: Domain Model Constructor                  │
│  Safety-net null/blank guards                       │
│  Rejects: invariant violations that should          │
│  never reach this point                             │
│  Response: HTTP 500 (programming bug if fires)      │
└─────────────────────────────────────────────────────┘
```

---

## Layer 1 — Controller / Entrypoint

**Responsibility:** Reject malformed input before it reaches the application core.

**Where:** Request DTOs (`@Valid`), custom Jakarta `@Constraint` annotations in `entrypoint/validation/` package.

**What belongs here:**
- Field presence (`@NotBlank`, `@NotNull`)
- String length (`@Size(max = 255)`)
- Format validation (`@Email`, E.164 phone format via custom constraint)
- Positive numeric values (`@Positive`)

**What does NOT belong here:**
- Business rules (use Layer 2)
- DB queries

**HTTP response:** 400 Bad Request

---

## Layer 2 — UseCase / Service

**Responsibility:** Enforce business rules before constructing domain objects. The service receives a Command, validates all rules that require reasoning or external state, then constructs the domain model.

**Where:** `*Service` classes in `core/domain/service/`.

**What belongs here:**
- Structural rules within an aggregate (e.g. duplicate `contactType + priority` in the contact list — a business invariant, not a format error)
- Uniqueness checks against the DB (e.g. contact value already exists for this debtor)
- Resource existence checks (e.g. debtor not found by ID)
- State transition guards (e.g. cannot cancel an already-paid invoice)
- Cross-aggregate rules
- External service pre-conditions

**HTTP response:** 422 Unprocessable Entity or 404 Not Found

---

## Layer 3 — Domain Model Constructor

**Responsibility:** Safety net. Ensures a domain object can never be instantiated with obviously broken state (null required field, blank name, etc.).

**Where:** Constructors of all domain model classes (`Debtor`, `DebtorContact`, etc.) via `DomainValidation`.

**What belongs here:**
- Null checks on required fields
- Blank checks on required strings

**What does NOT belong here:**
- Format validation (email, phone) — Layer 1
- Business rules (duplicate checks, state guards) — Layer 2
- DB queries

**HTTP response:** 500 Internal Server Error

**Key principle:** If this layer fires during a real request, it is a **programming bug** — Layer 1 or Layer 2 failed to validate before reaching here. This is intentional: the 500 signals a broken pipeline, not a user error.

> Reference: [Always Valid Domain Model — Vladimir Khorikov](https://vkhorikov.medium.com/always-valid-domain-model-706e5f3d24b0)
