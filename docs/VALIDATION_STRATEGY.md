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

## Required Request Headers (pre-Layer-1)

Every request — before it reaches any `@CaimanEndpoint` controller — is checked by `RequiredHeaderFilterConfig` (`caiman-web-support`, `@Order(2)`), a servlet filter that runs ahead of Spring MVC dispatch. It is not one of the three validation layers above; it rejects the request at the transport level, before a `Command` or even a `RequestDto` exists.

**Required on every request:**
- `X-Correlation-ID` — must be present, non-blank, and a valid UUID (`Constants.UUID_FORMAT`).
- `X-Channel` — must be present and non-blank.

**Response:** `400 Bad Request` (via `EntrypointException`), same `ProblemDetailDto` shape as Layer 1, with `source.header` pointing at the missing/invalid header. Both `correlationId` and `channel` are then echoed back at the root of every error response body for the rest of the request's lifecycle.

**Exempt paths:** `/favicon.ico`, the management/actuator base path, OpenAPI docs, and Swagger UI — see `RequiredHeaderFilterConfig.ignoredPaths`. No other path is exempt.

**Caveat — public/tokenized-link endpoints:** endpoints meant to be hit directly from an email link (`GET/POST /public/proofs`, `caiman-payment`, design in progress — see `docs/superpowers/specs/2026-09-02-public-proof-upload-page-design.md`) must be added to `ignoredPaths`. A browser following a plain link sets no custom headers, so `GET /public/proofs` would otherwise be rejected with 400 before ever reaching the controller — this is a hard requirement for that spec, not optional. `POST /public/proofs` is called via `fetch()` from the page's own JS and could technically set these headers, but is exempted too for consistency (an anonymous debtor has no meaningful `X-Correlation-ID`/`X-Channel` to send).

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

---

## HTTP Error Response Format ([RFC 9457](https://www.rfc-editor.org/info/rfc9457/))

All layers above (400, 422, 500) share the same error response shape. Represented by `ProblemDetailDto` in `web-support`, built on top of RFC 9457 "problem details".

**Fields:**
- `type` — omitted. Per RFC 9457 §3.1.1, absence is assumed to mean `about:blank` (generic problem, no dedicated doc site to point to).
- `status` — HTTP status code.
- `title` — fixed, generic per problem family (not per specific rule).
- `detail` — fixed, generic sentence pointing to `errors` for specifics.
- `instance` — the request's `requestId`, as an absolute path (`/requests/{id}`). Opaque identifier, not required to be dereferenceable.
- `correlationId` / `channel` — propagated from request headers, request-scoped (root level, not repeated per error).
- `errors` — array of `ProblemErrorDto`, one per violation. Present regardless of whether there's 1 or N violations — same shape for fail-fast and accumulated (Notification-pattern) validation.

**`ProblemErrorDto` fields:**
- `code` — full `ExceptionCode` (`{MODULE_PREFIX}_{code}`), identifies exactly which rule/invariant failed.
- `message` — **always present**. Static, generic text for this `code` (`ExceptionCode.getMessage()`), derived at mapping time — never stored per instance, always the same text for the same `code`.
- `detail` — **optional**, omitted entirely when there's nothing beyond what `message` already says. Present only when there's extra runtime-specific context (e.g. Jakarta's own generated constraint message, or specific IDs that failed a lookup). Never overwrites `message` — the two are independent fields, shown side by side when both apply.
- `source` — **optional**, omitted entirely when the violation isn't tied to a single request element (e.g. a business rule like "cannot cancel an already-paid invoice", a cross-field invariant). When present, exactly one of:
    - `body` — JSONPath ([RFC 9535](https://www.rfc-editor.org/rfc/rfc9535)) to the offending field, e.g. `$.contacts[*].value`. The only one of the four that needs path syntax, since it's the only nested structure.
    - `header` — header name, e.g. `X-Correlation-Id`.
    - `parameter` — query param name, e.g. `page`.
    - `pathParameter` — path param name, e.g. `debtorId`.
    - `invalidValue` — **optional**, the actual value that was rejected, alongside whichever location key above applies. Omit for sensitive fields (passwords, tokens, documents/PII) — not automatic for every field, must be an explicit allowlist.

No root-level `code` — redundant with `errors[].code`; with N errors there's no single value to put there anyway.

### Example — multiple accumulated violations (422, duplicate contact value)

One `ValidationError` per offending array element, not one concatenated string. Each points to its exact index via `source.body`, with the actual duplicated value in `source.invalidValue` — no wildcard (`[*]`), since the exact positions that collided are already known when the violation is detected.

Two contacts share the same `value` (`tst@tst.com`), one at index `0`, the other at index `1`:

```json
{
    "status": 422,
    "title": "Business rule violation",
    "detail": "One or more business rules were violated. See 'errors' for details.",
    "instance": "/requests/d290f1ee-6c54-4b01-90e6-d701748f0851",
    "correlationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "channel": "WEB",
    "errors": [
        {
            "code": "DEBTOR_BUSINESS_001",
            "message": "Informed contact list has duplicate contact value",
            "source": { "body": "$.contacts[0].value", "invalidValue": "tst@tst.com" }
        },
        {
            "code": "DEBTOR_BUSINESS_001",
            "message": "Informed contact list has duplicate contact value",
            "source": { "body": "$.contacts[1].value", "invalidValue": "tst@tst.com" }
        }
    ]
}
```

Same `code` reused across both entries (same rule violated). No `detail` here — `message` (static, from the enum) already says enough, and the specific offending value lives in `source.invalidValue`. No string concatenation needed.

### Example — single fail-fast violation (422, not tied to a request field)

```json
{
    "status": 422,
    "title": "Business rule violation",
    "detail": "One or more business rules were violated. See 'errors' for details.",
    "instance": "/requests/f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "correlationId": null,
    "channel": null,
    "errors": [
        {
            "code": "BILLING_BUSINESS_001",
            "message": "One or more of the informed debtors were not found",
            "detail": "The given debtor IDs were not found: [a1b2c3d4-...]"
        }
    ]
}
```

Here `detail` earns its place — it carries the specific IDs that failed, which `message` (generic, fixed) can't.

### Example — field validation failure (400, `@NotBlank`)

Layer 1 uses a single fixed `code` (`WebSupportExceptionCode.INVALID_VALUES`) for every constraint type — no per-constraint-type code mapping. `message` is therefore always the same generic text too, since it's derived from `code`. The specific, per-occurrence text (Jakarta's own generated constraint message) lives entirely in `detail`.

```json
{
    "status": 400,
    "title": "Field validation failed",
    "detail": "One or more request fields are invalid. See 'errors' for details.",
    "instance": "/requests/9b2b1c3a-1a2b-4c3d-8e5f-6a7b8c9d0e1f",
    "correlationId": null,
    "channel": "WEB",
    "errors": [
        {
            "code": "WEB_SUPPORT_002",
            "message": "Some invalid values were sent",
            "detail": "must not be blank",
            "source": { "body": "$.name", "invalidValue": "" }
        }
    ]
}
```

### Example — mixed sources (400, body + query param + path param + header)

Same `code`/`message` across all 4 entries (same generic code, regardless of constraint type or origin) — only `detail` and `source` vary per violation.

```json
{
    "status": 400,
    "title": "Field validation failed",
    "detail": "One or more request fields are invalid. See 'errors' for details.",
    "instance": "/requests/3f2504e0-4f89-11d3-9a0c-0305e82c3301",
    "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "channel": "WEB",
    "errors": [
        {
            "code": "WEB_SUPPORT_002",
            "message": "Some invalid values were sent",
            "detail": "must not be blank",
            "source": { "body": "$.name", "invalidValue": "" }
        },
        {
            "code": "WEB_SUPPORT_002",
            "message": "Some invalid values were sent",
            "detail": "must be a positive integer",
            "source": { "parameter": "page", "invalidValue": "-1" }
        },
        {
            "code": "WEB_SUPPORT_002",
            "message": "Some invalid values were sent",
            "detail": "must be a valid UUID",
            "source": { "pathParameter": "debtorId", "invalidValue": "not-a-uuid" }
        },
        {
            "code": "WEB_SUPPORT_002",
            "message": "Some invalid values were sent",
            "detail": "must be a valid ISO 639-1 language code",
            "source": { "header": "Accept-Language", "invalidValue": "xx" }
        }
    ]
}
```

### Example — unexpected error (500, `TechnicalException`)

```json
{
    "status": 500,
    "title": "Internal Server Error",
    "detail": "An unexpected error occurred. Please contact support if the problem persists.",
    "instance": "/requests/d290f1ee-6c54-4b01-90e6-d701748f0851",
    "correlationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "channel": "WEB",
    "errors": null
}
```

`errors` is always `null` for every 500 response, with no exception — even `TechnicalException`, whose `ValidationError` list only ever contains the generic `UNEXPECTED_ERROR` entry (never anything sensitive). Simpler and safer to apply the rule uniformly by HTTP status alone, rather than special-casing "this one 500 happens to be safe to show." Never leaks the real exception (e.g. a `NullPointerException`'s class/message/stacktrace) into the response — no `detail`, no `source`, no `errors` at all. Only `instance`/`correlationId` are available to correlate against server logs. The real exception (full stacktrace, cause, and the actual `ValidationError` list) is only ever logged server-side via `executeLogging()`, never serialized to the client. Same rule applies to `DomainException` (500, invariant violated).
