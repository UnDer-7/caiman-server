# Public Proof Upload Page — Design

**Date:** 2026-09-02
**Status:** Approved for implementation (no implementation plan requested — build directly from this spec)
**Bounded context:** `caiman-payment` (+ a new `InvoiceGateway` contract shared with `caiman-billing`)

## 1. Goal

The debtor receives an `INVOICE_CREATED` (or reminder) email with a link (`uploadLink`, §10.6 `BUSINESS_RULES.md`). That link must let them, with no login and no client app:

1. `GET` the link → see a page with the invoice's data (plan name, debtor name, amount due/paid, due date) and, if the invoice still accepts a proof, an upload form.
2. `POST` from that form → submit a proof file plus a declared paid amount (total or partial).

This spec covers the full `GET` (real, working) and the `POST` **controller shape only** — request/response contract, structural validation, and logging. The actual business rules behind the upload (§7.1 conflict checks, §7.3 `payment_proof` creation, §8 AI analysis) are explicitly deferred — see Non-goals.

**Why this page lives in `caiman-server` and not the future `caiman-web-client`:** `caiman-web-client` (planned, separate repo, React + Spring Boot) is an **admin-only** SPA. The design principle is that an admin can run `caiman-server` standalone via its API, with zero dependency on `caiman-web-client`. If the debtor's email link pointed at `caiman-web-client`, the admin would be forced to also run that second service just for debtors to pay — breaking that independence. So the one HTML page in this backend is the debtor-facing proof upload flow.

## 2. Non-goals (explicit)

- No real `POST` business logic: no `payment_proof` persistence, no §7.1 conflict checks (409 on active proof / paid / cancelled invoice), no AI trigger, no events. The controller logs the accepted request and returns a fixed stub response.
- No file storage. The uploaded `MultipartFile` is read into the request only; nothing is written to disk in this spec.
- No JWT, no signed/expiring tokens (see §4 below — deliberately dropped).
- No rate limiting / brute-force protection on the token lookup — out of scope, entropy of a UUID v4 is the only protection, same as every other magic-link system.
- No changes to `payment_proof` DDL (`token_expires_at` becomes vestigial once tokens stop expiring, but the column is left alone — `payment_proof` has no entity/repository yet, so there is nothing to migrate off of it today; note it for whoever writes that entity).

## 3. Documentation corrected while writing this spec

Two pre-existing contradictions were found and fixed directly in the docs (not just here), matching the project convention:

- **JWT/expiry was never going to be implemented and is now removed everywhere:** `BUSINESS_RULES.md` (glossary "Upload Token", §7.1 endpoint contract, §10.6 payload example, §12.1 resend-link), `MODULE_FLOW.md` (sequence diagram), architecture overview's Authentication bullet. Rationale: the token deliberately never expires — a debtor must be able to pay off an old invoice using the original email link no matter how much later they act, so a 48h JWT expiry actively worked against the product's own use case. The already-implemented mechanism (`invoice.upload_token`, a persisted `UUID`, unique per invoice, never rotated — `InvoiceEntity.java:86`, index `uq_invoice_upload_token`) is simpler and was already correct; only the docs were stale.
- **Route shape:** the doc's `{invoiceId}` path segment is redundant — the token is already the unique lookup key (`UNIQUE(upload_token)`), so there's nothing to cross-check it against. Route simplified to `/public/proofs?token={uuid}` everywhere (`BUSINESS_RULES.md` §7.0/§7.1, `AGENT.md` endpoint list, `MODULE_FLOW.md`).
- **`AGENT.md`'s "no HTML" rule amended:** added an explicit, narrow exception for this one page (Thymeleaf only, only here) — see §5 below. The rest of the backend stays JSON-only.
- **`VALIDATION_STRATEGY.md`'s already-documented caveat resolved into a hard requirement:** `RequiredHeaderFilterConfig.ignoredPaths` (`caiman-web-support/.../filter/RequiredHeaderFilterConfig.java`) currently rejects any request without `X-Correlation-ID` and `X-Channel` with a `400` — before Spring MVC dispatch, before any controller runs. A debtor clicking a plain email link sends neither header. **`/public/proofs` must be added to `ignoredPaths`** or the entire flow 400s before reaching the controller. This is not optional and is called out again in §8.

## 4. Token scheme (unchanged, documented for clarity)

`invoice.upload_token` — `UUID`, generated once at invoice creation (`caiman-billing`), stored on `invoice` (`upload_token` column, `UNIQUE` index `uq_invoice_upload_token`). Never rotated, never expires. A resend (§12.1) reuses the same token — it does not generate a new one. Validation is a single lookup: does an invoice exist with this `upload_token`? No signature, no claims, no expiry check.

## 5. `AGENT.md` amendment (already applied)

```
**This repository is the backend API server only.** [...] This project has no admin
frontend and no template rendering framework for JSON endpoints.

**Exception — public payment-proof upload page:** the debtor-facing proof upload flow
(`GET/POST /public/proofs`) is the one place this backend serves HTML. [...] This page
uses Thymeleaf (server-rendered, no JS framework/build step) and is the only controller
in the project allowed to return HTML instead of JSON.
```

`caiman-payment:entrypoint` gains `spring-boot-starter-thymeleaf` as a dependency — no other module does.

## 6. New contract — `InvoiceGateway` (`caiman-contracts`)

Same shape as the existing `DebtorGateway` (`caiman-shared/contracts/.../gateway/debtor/DebtorGateway.java`). New package `com.caimanproject.contracts.gateway.invoice`:

```java
package com.caimanproject.contracts.gateway.invoice;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceGateway {

    /**
     * @param token the invoice's upload_token
     * @return a snapshot of the invoice (and its plan/debtor context), empty if no invoice has this token
     */
    Optional<InvoiceSnapshotDto> findByUploadToken(UUID token);
}
```

```java
package com.caimanproject.contracts.gateway.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record InvoiceSnapshotDto(
        UUID id,
        String chargePlanName,
        String debtorName,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        String status, // invoice.status as string — payment's core has no reason to depend on billing's InvoiceStatus enum
        Instant dueDate,
        int cycleIndex) {}
```

Implemented by a new `InvoiceQueryAdapter` in `caiman-billing:infrastructure` (query-only — follows the read/write port split convention, `AGENT.md` "Interface segregation"), wired as a bean in `caiman-app` and injected into `caiman-payment:core`. Cross-module dependency direction matches every other gateway in the project: `caiman-payment:core → caiman-contracts ← caiman-billing:infrastructure`, no direct module-to-module dependency.

## 7. `caiman-payment:core` — GET use case

Follows the project's hexagonal conventions (`AGENT.md` — Command objects, `port.in`/`port.out`, `*Service` naming).

```
core/port/in/
  GetProofPageUseCase.java
  command/
    GetProofPageCommand.java        // record(UUID token)
core/port/out/
  ActiveProofExistsGateway.java     // own bounded-context data, not a cross-module gateway
core/domain/service/
  GetProofPageService.java
core/domain/model/
  ProofPageView.java                // the view model handed to the entrypoint
```

`ActiveProofExistsGateway` is **not** a `caiman-contracts` gateway — `payment_proof` is owned by `caiman-payment` itself, so this is a normal in-context port, implemented by a JPA adapter in `caiman-payment:infrastructure` once the `payment_proof` entity exists.

```java
public interface ActiveProofExistsGateway {
    boolean existsActiveProof(UUID invoiceId); // status NOT IN (REJECTED) — mirrors §7.1
}
```

`GetProofPageService.handle(GetProofPageCommand)`:

1. `invoiceGateway.findByUploadToken(command.token())` → empty ⇒ throw a `NotFoundException`-family `CaimanException` (mapped to 404 by the entrypoint's local handler, §9).
2. `activeProofExistsGateway.existsActiveProof(invoice.id())`.
3. `formEnabled = invoice.status() not in (PAID, CANCELLED) && !activeProofExists`.
4. Build and return `ProofPageView` (invoice snapshot fields + `formEnabled` + a `statusMessage` when `formEnabled == false`, e.g. `"This invoice has already been paid."` / `"A proof is already under review for this invoice."`).

## 8. Entrypoint — routes and templates

`caiman-payment:entrypoint`, package `entrypoint.controller`, single `PublicProofController` (`@CaimanEndpoint`, but note: `@CaimanEndpoint` is `@RestController` — this controller's `GET` method returns `String` (view name) with a `Model` param and must instead be a plain `@Controller` + `@RequestMapping`, or `@CaimanEndpoint` needs a variant. **Open implementation detail, not resolved by this spec** — whoever implements this checks whether `@CaimanEndpoint` can coexist with view resolution or whether this one controller opts out of it and wires path-prefixing manually.)

Routes:

| Method | Path | Produces |
|---|---|---|
| `GET` | `/public/proofs?token={uuid}` | `text/html` |
| `POST` | `/public/proofs?token={uuid}` | `application/json` (stub) |

**Template:** one Thymeleaf file, `templates/proof-page.html`, covering all three `GET` states (form / informational / — the 404 case uses a second, minimal `templates/proof-not-found.html`, since it has no invoice data to show). Inline `<style>`/`<script>` in the same file — no separate CSS/JS build step, no bundler, matches the "loads fast, no framework" goal.

Page content when `formEnabled = true`:
- Plan name, debtor name, amount due, amount paid, due date, cycle index.
- Radio: **"Pagamento Total"** (default, selected) / **"Pagamento Parcial"**. Selecting "Parcial" reveals a required numeric input (`declaredAmount`, `> 0`). "Total" hides it — the page does not send a client-computed amount for the total case; the amount is implied by the radio choice alone (server already knows `amountDue` from the token lookup).
- File input (proof), submit button.
- Submit handler: `fetch('/public/proofs?token=' + token, { method: 'POST', body: formData })` (`FormData`, `multipart/form-data`, browser sets the boundary automatically). On success, replace the form with a confirmation message. On error, show the JSON error's `errors[].detail`/`message` inline, no reload.

Page content when `formEnabled = false`: same layout, invoice data shown, form section replaced by `statusMessage`.

## 9. `POST` — controller-only contract (per this spec's explicit scope)

Request (`multipart/form-data`):

| Field | Type | Rule |
|---|---|---|
| `file` | file part | required; content-type in `{image/jpeg, image/png, image/webp, application/pdf}`; max size per `application.properties` (reuse the same 10MB limit already defined in §7.2 of `BUSINESS_RULES.md`, even though nothing is stored yet — this is Layer 1 structural validation, not business logic) |
| `paymentType` | string | required; `TOTAL` \| `PARTIAL` |
| `declaredAmount` | decimal | required and `> 0` only if `paymentType = PARTIAL`; absent/ignored if `TOTAL` |

Controller behavior:

1. `UUID.fromString(token)` — malformed → `404` (same not-found path as an unknown token; don't distinguish "bad format" from "not found" to an anonymous caller).
2. `invoiceGateway.findByUploadToken(token)` — empty → `404`. (Only used to resolve `invoiceId` for the log line below — no §7.1 conflict/state checks yet, per Non-goals.)
3. Log one structured line (same pattern as `NotifyInvoiceCreationAdapter`, `StructuredArguments`): `invoiceId`, `paymentType`, `declaredAmount` (nullable), original filename, content-type, size in bytes. **Never log the file's bytes.**
4. Return `202 Accepted` with a fixed stub body: `{ "message": "Received. Proof handling is not implemented yet." }`.

No `payment_proof` row, no event, no AI call — this is explicitly a placeholder for a future spec/implementation to fill in with the real §7.3–§9 flow.

## 10. Error handling

**Why `GET` and `POST` diverge:** `GlobalRestExceptionHandlerConfig` (`caiman-web-support`) renders JSON. That's correct for `POST` (called via `fetch`, JS parses and displays the error inline) but wrong for `GET` (a real browser navigation — a debtor with no dev tools must never see raw JSON).

- **`GET`:** local `@ExceptionHandler` on `PublicProofController`, HTML only:
  - not-found (bad token format or no match) → `404`, renders `proof-not-found.html`.
  - anything else unexpected (e.g. `InvoiceGateway` call fails because billing is down) → `500`, renders a generic HTML error template, vague message, no stack trace — same "never leak the real exception" rule as `VALIDATION_STRATEGY.md`'s Layer 3, just rendered as HTML instead of `ProblemDetailDto`.
- **`POST`:** falls through to the existing global JSON handler unchanged. No new handler needed.

**`RequiredHeaderFilterConfig.ignoredPaths` — mandatory infra change**, not optional (§3 above): add `/public/proofs` (both methods hit the same path) alongside the existing exemptions (`/favicon.ico`, actuator, OpenAPI/Swagger). Without this, `RequiredHeaderFilterConfig` (`@Order(2)`, runs before Spring MVC) rejects every plain-browser `GET` with `400` before the controller — and every `POST`, unless the page's own `fetch()` is made to fabricate `X-Correlation-ID`/`X-Channel` values, which is unnecessary complexity for an anonymous debtor request.

## 11. Security notes

- No authentication by design (matches every other debtor-facing flow — "no login" is an explicit project principle, `BUSINESS_RULES.md` Design Principles).
- Security of the link rests entirely on `UUID` v4 entropy — same trust model as the token already had before this spec; nothing regresses.
- Thymeleaf: all dynamic text (`chargePlanName`, `debtorName`, `statusMessage`) rendered via `th:text` (auto-escaped) — never `th:utext` — to avoid stored XSS through any admin-entered free-text field (plan name, debtor name) reaching an anonymous debtor's browser unescaped.

## 12. Testing

- **Unit** (`@UnitTest`): `GetProofPageService` — form-enabled decision matrix (`PAID`/`CANCELLED`/active-proof/none combinations), not-found path. No Spring context.
- **Integration** (`@IntegrationTest`):
  - `GET` with a valid open token → `200`, page contains the invoice's amount/plan/debtor name and the form.
  - `GET` with a valid but non-uploadable token (paid / cancelled / active proof) → `200`, form absent, status message present.
  - `GET` with an unknown/malformed token → `404`, `proof-not-found.html`.
  - `GET` without `X-Correlation-ID`/`X-Channel` headers still succeeds (proves the `ignoredPaths` fix, §10) — this is the one place in the whole test suite where *not* sending those headers is the point of the test.
  - `POST` with a well-formed multipart request → `202`, stub body; log line asserted via a captured `Logback` appender (present in the codebase for other structured-log assertions — reuse that harness, not a new one).
  - `POST` with an unknown token → `404`, standard JSON error body.
  - `POST` with `paymentType=PARTIAL` and missing/zero `declaredAmount` → `400`, standard JSON error body (Layer 1).

## 13. Risks / deferred items

- **GraalVM native image (`AGENT.md` rule 15):** Thymeleaf's native-image reachability is not verified as part of this spec. Before `nativeCompile` is attempted with this page in place, check the GraalVM Reachability Metadata Repository for `org.thymeleaf:thymeleaf-spring6` (or whatever artifact the Spring Boot 4 BOM resolves); if absent, manual `RuntimeHintsRegistrar` work is needed (template resource loading via `hints.resources()`, at minimum).
- **`@CaimanEndpoint` vs. view-returning controller** (§8) — needs a decision at implementation time, not resolved here.
- Real `POST` business logic (§7.1/§7.3/§8/§9 of `BUSINESS_RULES.md`) is a separate future spec/implementation — this one only defines its controller shape and logging.
- `payment_proof.token_expires_at` is now meaningless (tokens don't expire) but the column isn't touched — no entity exists yet to migrate off it; flagged for whoever builds the real `payment_proof` persistence.
