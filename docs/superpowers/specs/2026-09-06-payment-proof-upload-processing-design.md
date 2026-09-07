# Payment Proof Upload — Processing Design

**Date:** 2026-09-06
**Status:** Draft — pending user review
**Bounded context:** `caiman-payment` (+ small, additive changes to `caiman-contracts` and `caiman-app`)

## 1. Goal

`PublicProofController#uploadProof` (`caiman-payment:entrypoint`) currently only logs the
upload and returns a hardcoded stub (`ProofUploadResponseDto("Received. Proof handling is
not implemented yet.")`). This spec implements the real processing behind it, per
`BUSINESS_RULES.md` §7.1–§7.3 and §8.1–§8.2:

1. Persist the uploaded file to disk (not the database — see §3) with an
   `payment_proof` record referencing it.
2. Resolve the proof's initial `status` via a `ProofValidationStrategy`, selected by
   `charge_plan.proof_validation_mode`. Only `MANUAL` is implemented; `AI_AUTO` and
   `AI_ASSISTED` are wired but throw `UnsupportedOperationException` (§8).
3. Return a response message tailored to the resulting `PaymentProofStatus`, since the
   `proof-page.html` client-side script displays this message verbatim to the debtor.

## 2. Non-goals (explicit)

- **AI analysis itself.** `AI_AUTO`/`AI_ASSISTED` strategies are stubs that throw — no
  Anthropic API call, no async background thread, no prompt contract. `// TODO` comment
  marks each for future implementation.
- **Payment registration (§9), admin manual review resolution (§9.3), payment-result
  notifications (§9.4).** None of these are reachable from this spec's code path: `MANUAL`
  always resolves to `PENDING_MANUAL_REVIEW` at upload time, never synchronously to
  `APPROVED`/`REJECTED`. Those sections govern a *separate*, later admin-facing endpoint
  (`POST /admin/proofs/{id}/resolve`), out of scope here.
- **`GET /public/proofs` behavior.** Unchanged — already implemented (`GetProofPageService`).

## 3. File storage — filesystem, not database

**Decision:** store the file on disk under a configured root directory; `payment_proof`
keeps a `file_path` column pointing at it. This matches what `BUSINESS_RULES.md` §7.2
already specified and what `V0_06__payment_proof.yaml` already modeled (`file_path`
column exists, unused until now) — no schema change needed for this column.

Rejected alternative: storing the file bytes as a DB blob (`bytea`/`BLOB` column). Works
technically (`byte[]` + `@Lob` avoids Postgres OID pitfalls and maps cleanly to both
dialects), but was dropped because:
- It requires a real schema change for no functional benefit here (10MB files, single
  self-hosted instance — no multi-instance/shared-nothing requirement forcing DB storage).
- It duplicates what `BUSINESS_RULES.md` already documented as filesystem-based.
- It bloats DB size/backup size and, for SQLite specifically, WAL growth, for zero gain.

### 3.1 Storage location & config

New config, following the existing `CAIMAN_SERVER_LOGGING_FOLDER_PATH` naming style:

| Env var | Purpose | Local dev default |
|---|---|---|
| `CAIMAN_SERVER_PAYMENT_PROOF_STORAGE_FOLDER_PATH` | Root directory for stored proof files | `./proof_storage` |

Added to `.env` (committed, safe — it's a path, not a secret) per `AGENT.md`'s env var
rule. Docker default: `/app/data/proofs` (see §8 below — reuses the volume mount point
both Dockerfiles already declare for `/app/data`).

`caiman-app/src/main/resources/application.yaml` wires the env var into the binding,
same pattern as every other `caiman-server.*` property:

```yaml
caiman-server:
  payment:
    proof-storage-folder-path: ${CAIMAN_SERVER_PAYMENT_PROOF_STORAGE_FOLDER_PATH}
```

`CaimanServerProps` (`caiman-contracts` — the interface `caiman-payment:infrastructure`
actually depends on and injects; `caiman-app`'s `CaimanServerPropsConfig` is its only
implementation) gains a new accessor and nested interface, same shape as the existing
ones:

```java
PaymentProp payment();

interface PaymentProp {
    String proofStorageFolderPath();
}
```

Wired into `CaimanServerPropsConfig` (`caiman-app`) as a new nested record:

```java
public record PaymentPropImpl(@NotBlank String proofStorageFolderPath)
        implements CaimanServerProps.PaymentProp {}
```

Added as a new top-level field `payment` on `CaimanServerPropsConfig`, alongside
`database`, `logging`, etc. **Required maintenance** (per the class's own javadoc):
`CaimanRuntimeHints.registerHints` must add
`.registerType(CaimanServerPropsConfig.PaymentPropImpl.class, MemberCategory.values())` —
otherwise Hibernate Validator's `JPATraversableResolver` throws
`MissingReflectionRegistrationError` under native image.

### 3.2 Startup validation — fail fast on a bad storage path

A plain `@NotBlank` on the path string only checks it's non-empty, not that the app can
actually use it. New component, `caiman-payment:infrastructure`,
`config/PaymentProofStorageInitializer.java`, implementing `InitializingBean`:

1. `Files.createDirectories(path)` — create it (and parents) if missing. If this throws
   (e.g. parent is a file, permission denied on parent), fail here.
2. `Files.isReadable(path)` and `Files.isWritable(path)` — if either is `false`, throw
   `IllegalStateException` with a message naming the exact path and which check failed
   (e.g. `"Payment proof storage folder '/app/data/proofs' is not writable. Check the
   directory's permissions or CAIMAN_SERVER_PAYMENT_PROOF_STORAGE_FOLDER_PATH."`).

An exception from an `InitializingBean` fails Spring Boot startup — the app does not come
up, mirroring the existing `DatabasePropImpl` fail-fast pattern (`AGENT.md`, "Database
Configuration" — that one rejects the app for bad env vars; this one rejects it for a
filesystem dependency it needs but can't use). No new AOT hints needed — `java.nio.file.*`
calls involve no reflection.

### 3.3 Filename generation

Format:

```
{debtorName}_{chargePlanName}_{cycleIndex}_{yyyyMMdd}_{HHmmss}_UTC_{uuid}.{ext}
```

Example: `MateusSilva_SharedNetflix_3_20260906_143205_UTC_9f1c2e3a-....jpg`

Rules:
- `debtorName` / `chargePlanName`: **sanitized** — strip to `[a-zA-Z0-9-]`, collapse
  repeated separators, cap at ~40 chars each. These are admin-entered free-text fields
  (`debtor.name`, `charge_plan.name`); an unsanitized slash or `..` would corrupt the
  path or enable path traversal. Non-negotiable, not just cosmetic.
- `cycleIndex`: `invoice.cycleIndex`, used for both `ROTATING` and `SPLIT` (always `0` for
  the latter) — avoids needing `chargePlanType` just for this.
- Date/time: always `UTC` literal (never server-local zone), consistent with the
  project's "all timestamps are UTC" rule (`AGENT.md` rule 2).
- `uuid`: `UUID.randomUUID()` — guarantees no collision even with identical debtor/plan/
  timestamp (e.g. two uploads in the same second, which sub-second granularity here
  can't rule out).
- Extension: **derived from the validated `content-type`** (`image/jpeg`→`jpg`,
  `image/png`→`png`, `image/webp`→`webp`, `application/pdf`→`pdf`), never from the
  client-supplied original filename. The content-type is already allowlist-checked in
  `PublicProofController#validateUpload`; trusting the client's filename extension
  directly is not.

Filename construction is a storage *detail*, not a domain rule — it lives in the
infrastructure adapter (§6.3), not in `core`.

## 4. Architecture correction — `PaymentProofStatus` belongs in `core`

`PaymentProofStatus` currently lives in
`caiman-payment/infrastructure/.../database/entity/PaymentProofStatus.java`. The new
`ProofValidationStrategy` (core) must return this enum, and `core` cannot depend on
`infrastructure` (`AGENT.md` — "Spring dependency boundary in core modules"). **Move it**
to `caiman-payment/core/.../domain/types/PaymentProofStatus.java`. `PaymentProofEntity`
(infra) references the relocated core enum directly — same pattern already used for
`InvoiceStatus` (billing) and other status enums that live in `core` and get consumed by
the JPA entity in `infrastructure`.

No value changes: `PENDING_ANALYSIS`, `PENDING_MANUAL_REVIEW`, `APPROVED`, `REJECTED`.

## 5. Cross-module data — `proof_validation_mode` reaches `caiman-payment`

`charge_plan.proof_validation_mode` lives in `caiman-billing`. `caiman-payment:core`
needs it to select a strategy, but cross-context data must go through `caiman-contracts`
(`AGENT.md` — "Cross-context communication"). The existing `InvoiceGateway` /
`InvoiceSnapshotDto` (added by the prior GET-page spec) already crosses this exact
boundary — extend it instead of adding a second gateway call.

`InvoiceSnapshotDto` (`caiman-contracts`) gains one field, typed `String` — same
precedent as the existing `status` field, whose doc comment already states the rationale
("payment's core has no reason to depend on billing's \[enum\] enum"):

```java
public record InvoiceSnapshotDto(
        UUID id,
        String chargePlanName,
        String debtorName,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        String status,
        String proofValidationMode, // new — charge_plan.proof_validation_mode as a string
        Instant dueDate,
        int cycleIndex) {}
```

`InvoiceSnapshotMapper` (`caiman-billing:infrastructure`) adds:

```java
@Mapping(target = "proofValidationMode", source = "chargePlan.proofValidationMode")
```

MapStruct converts the billing-core `ProofValidationMode` enum to `String` automatically
(built-in enum→String support), same as it already does for `status`.

`caiman-payment:core` defines its **own** `ProofValidationMode` enum (mirrors billing's
three values: `AI_AUTO`, `MANUAL`, `AI_ASSISTED`) in `domain/types/`, and parses the
string via `ProofValidationMode.valueOf(invoice.proofValidationMode())` where the
strategy is selected (§7.2). Same "each context owns its own vocabulary, strings cross
the boundary" convention already established for invoice status.

## 6. `caiman-payment:core` — domain model

Follows the project's domain-model conventions (`AGENT.md` — `createBuilder`/
`restoreBuilder`, invariant validation in the constructor, `DomainException` on
violation). Modeled directly on the already-existing `PaymentProofEntity`, plus the 3
new columns from §6.1.

### 6.1 `payment_proof` — DDL changes (edited in place)

Project is pre-release (no PROD environment yet — confirmed by user), so
`V0_06__payment_proof.yaml` is **edited directly**, not appended with a new changeset.
(Caveat for whoever runs this locally: if `payment_proof`'s changeset already ran against
a local dev DB, its checksum will now mismatch on the next `liquibase update` — drop/
recreate the local DB, or `liquibase clearCheckSums`, to pick up the edit.)

Added columns:

| Column | Type | Nullable | Purpose |
|---|---|---|---|
| `original_filename` | `varchar(255)` | `false` | Client-supplied filename, for admin display — the on-disk filename (§3.3) is synthetic. |
| `file_content_type` | `varchar(100)` | `false` | Validated MIME type, needed to serve/preview the file later without re-sniffing it. |
| `file_size_bytes` | `bigint` | `false` | Avoids a disk `stat()` just to show size in an admin UI. |

`file_path` (existing column) stores the path returned by `ProofFileStorageGateway`
(§7.3) — relative to the configured storage root, matching the doc comment already on
that column.

### 6.2 `Audit` (new, `caiman-payment/core/domain/model/Audit.java`)

`caiman-payment:core` has no `Audit` value object yet (only `GetProofPageService`/
`ProofPageView` exist, neither persists anything). Mirrors the existing pattern used
identically in `caiman-billing:core` and `caiman-debtor:core`:

```java
public class Audit {
    private final Instant createdAt;
    private final Instant updatedAt;

    @Builder
    public Audit(final Instant createdAt, final Instant updatedAt) { ... }
    public Audit() { this.createdAt = null; this.updatedAt = null; }

    public Optional<Instant> getCreatedAt() { return Optional.ofNullable(createdAt); }
    public Optional<Instant> getUpdatedAt() { return Optional.ofNullable(updatedAt); }
}
```

### 6.3 `PaymentProof` (new, `caiman-payment/core/domain/model/PaymentProof.java`)

Mirrors `Invoice`'s two-constructor pattern exactly.

```java
public class PaymentProof {
    private final UUID id;                      // null until persisted
    private final UUID invoiceId;
    private final String filePath;
    private final String originalFilename;
    private final String fileContentType;
    private final long fileSizeBytes;
    private final String uploadToken;            // the token used for this upload, for audit
    private final Instant tokenExpiresAt;         // vestigial (tokens never expire — see prior
                                                   // spec §13), column kept, always far-future
    private final BigDecimal aiExtractedValue;    // null — no AI path implemented yet
    private final BigDecimal finalValue;          // null until approved
    private final boolean requiresManualReview;
    private final String aiRawResponse;           // null — no AI path implemented yet
    private final PaymentProofStatus status;
    private final Audit audit;

    @Builder(builderMethodName = "restoreBuilder", builderClassName = "RestoreBuilder")
    public PaymentProof(/* all fields */) {
        // required-field invariant validation via DomainValidation.validateAll(...),
        // throwIfInvalid(DomainException::new) — same pattern as Invoice.
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public PaymentProof(
            final UUID invoiceId,
            final String filePath,
            final String originalFilename,
            final String fileContentType,
            final long fileSizeBytes,
            final String uploadToken,
            final PaymentProofStatus status,
            final boolean requiresManualReview) {
        this(null, invoiceId, filePath, originalFilename, fileContentType, fileSizeBytes,
             uploadToken, Instant.MAX, null, null, requiresManualReview, null, status, null);
    }

    public Optional<UUID> getId() { return Optional.ofNullable(id); }
    // ... Optional getters for nullable fields, same convention as Invoice
}
```

`status` and `requiresManualReview` are **inputs** to `createBuilder`, not computed
inside it — the strategy (§7) decides them before the domain object is built. The domain
model's job is just invariant enforcement (non-null invoiceId/filePath/status/etc.), not
deciding *which* status is correct — that decision depends on `proof_validation_mode`,
which is an application/use-case concern, not a domain invariant.

`requiresManualReview` semantics per `BUSINESS_RULES.md` §7.3/§8: always `true` when
`status == PENDING_MANUAL_REVIEW`, always `false` when `status == PENDING_ANALYSIS`. The
service (§7.3) sets it directly off the resolved status — no separate strategy method
needed for it.

### 6.4 `PaymentProofStatus` (relocated, see §4) and `ProofValidationMode` (new, see §5)

Both plain enums in `domain/types/`, no behavior.

## 7. Strategy pattern — `ProofValidationStrategy`

New package, `caiman-payment/core/domain/strategy/` (new subpackage — `AGENT.md`'s
`domain/service/` convention is for `*Service` use-case implementations; a strategy
interface with multiple interchangeable implementations doesn't fit that naming and gets
its own package instead).

### 7.1 Interface and context

```java
public interface ProofValidationStrategy {
    ProofValidationMode supportedMode();
    PaymentProofStatus resolveStatus(ProofValidationContext context);
}
```

```java
public record ProofValidationContext(
        byte[] fileContent,
        String fileContentType,
        String originalFilename,
        InvoiceSnapshotDto invoice,
        PaymentType paymentType,
        BigDecimal declaredAmount) {}
```

The context carries everything a *future* AI strategy will need (file bytes + invoice
context for the Anthropic prompt, per `BUSINESS_RULES.md` §8.3) even though nothing reads
those fields yet — this avoids re-shaping the interface when AI is implemented later.

### 7.2 Implementations

```java
@Component
public class ManualProofValidationStrategy implements ProofValidationStrategy {
    @Override public ProofValidationMode supportedMode() { return ProofValidationMode.MANUAL; }

    @Override
    public PaymentProofStatus resolveStatus(final ProofValidationContext context) {
        // BUSINESS_RULES.md §8.2 — MANUAL always goes straight to admin review,
        // no analysis step, regardless of file/amount/anything else.
        return PaymentProofStatus.PENDING_MANUAL_REVIEW;
    }
}

@Component
public class AiAutoProofValidationStrategy implements ProofValidationStrategy {
    @Override public ProofValidationMode supportedMode() { return ProofValidationMode.AI_AUTO; }

    @Override
    public PaymentProofStatus resolveStatus(final ProofValidationContext context) {
        // TODO: implement AI_AUTO analysis (BUSINESS_RULES.md §8.3/§8.4) — async Anthropic
        // API call, isValid routing to APPROVED/REJECTED, failure routing to
        // PENDING_MANUAL_REVIEW. Not implemented in this spec.
        throw new UnsupportedOperationException("AI_AUTO proof validation is not implemented yet");
    }
}

@Component
public class AiAssistedProofValidationStrategy implements ProofValidationStrategy {
    @Override public ProofValidationMode supportedMode() { return ProofValidationMode.AI_ASSISTED; }

    @Override
    public PaymentProofStatus resolveStatus(final ProofValidationContext context) {
        // TODO: implement AI_ASSISTED analysis (BUSINESS_RULES.md §8.3/§8.5) — async
        // Anthropic API call, isValid=true routes to PENDING_MANUAL_REVIEW for admin
        // confirmation, isValid=false routes to REJECTED. Not implemented in this spec.
        throw new UnsupportedOperationException("AI_ASSISTED proof validation is not implemented yet");
    }
}
```

### 7.3 Resolver

```java
@Component
@RequiredArgsConstructor
public class ProofValidationStrategyResolver {

    private final List<ProofValidationStrategy> strategies;
    private Map<ProofValidationMode, ProofValidationStrategy> byMode;

    @PostConstruct
    void index() {
        byMode = strategies.stream()
                .collect(Collectors.toMap(ProofValidationStrategy::supportedMode, Function.identity()));
    }

    public ProofValidationStrategy resolve(final ProofValidationMode mode) {
        final var strategy = byMode.get(mode);
        if (strategy == null) {
            throw new IllegalStateException("No ProofValidationStrategy registered for mode " + mode);
        }
        return strategy;
    }
}
```

Spring injects all three `@Component` strategies into the `List<ProofValidationStrategy>`
— adding a 4th mode later is a new `@Component`, zero changes to the resolver.

## 8. Conflict checks (`BUSINESS_RULES.md` §7.1, steps 4–5)

Included in this spec's scope (confirmed) — without them, a debtor could upload a second
proof while one is already pending, or upload against an already-paid/cancelled invoice.

1. **Active proof exists** — reuse `ActiveProofExistsGateway` (already built for the GET
   page, `existsActiveProof(invoiceId)` checks `status NOT IN (REJECTED)`). No new port.
2. **Invoice not payable** — `invoice.status()` (the `String` field already on
   `InvoiceSnapshotDto`) must not be `PAID` or `CANCELLED`.

**Doc correction:** `BUSINESS_RULES.md` §7.1 says these return `409 Conflict`. This
project's error framework (`ErrorHttpStatus`) only defines `400`/`404`/`422`/`500` —
there is no 409 exception type anywhere in the codebase, and the documented convention
(see `BusinessException`) is that business-rule violations are `422 Unprocessable
Entity`, not `409`. `BUSINESS_RULES.md` §7.1 is updated to say `422` instead of `409`,
same convention as the prior public-proof-page spec correcting other stale doc claims
(§3 of that spec).

New `BusinessExceptionCode` entries (`caiman-payment:core`):

```java
ACTIVE_PROOF_EXISTS("002", "A payment proof is already pending review for this invoice"),
INVOICE_NOT_PAYABLE("003", "This invoice no longer accepts a payment proof")
```

Both thrown as `BusinessException` (422) — the existing `INVOICE_NOT_FOUND` stays a
`NotFoundException` (404), unchanged.

## 9. `caiman-payment:core` — use case

```
core/port/in/
  UploadProofUseCase.java
  command/UploadProofCommand.java
  result/UploadProofResult.java
core/port/out/
  PaymentProofPersistenceGateway.java   // new
  ProofFileStorageGateway.java          // new
  command/StoreProofFileCommand.java    // new
  ActiveProofExistsGateway.java         // existing, reused as-is
core/domain/service/
  UploadProofService.java               // new
```

```java
public interface UploadProofUseCase {
    UploadProofResult execute(UploadProofCommand command);
}

public record UploadProofCommand(
        UUID token,
        byte[] fileContent,
        String fileContentType,
        String originalFilename,
        PaymentType paymentType,
        BigDecimal declaredAmount) {}

public record UploadProofResult(UUID proofId, PaymentProofStatus status) {}
```

```java
public interface PaymentProofPersistenceGateway {
    PaymentProof save(PaymentProof proof);
}
```

```java
public interface ProofFileStorageGateway {
    String store(StoreProofFileCommand command); // returns the relative file_path to persist
}
```

```java
public record StoreProofFileCommand(
        byte[] fileContent,
        String fileContentType,
        String debtorName,
        String chargePlanName,
        int cycleIndex) {}
```

`UploadProofService.execute(UploadProofCommand)`:

1. `invoiceGateway.findByUploadToken(command.token())` → empty ⇒ `NotFoundException`
   (existing behavior, unchanged).
2. `activeProofExistsGateway.existsActiveProof(invoice.id())` → `true` ⇒
   `BusinessException(ACTIVE_PROOF_EXISTS)`.
3. `invoice.status() in {"PAID", "CANCELLED"}` → `BusinessException(INVOICE_NOT_PAYABLE)`.
4. `mode = ProofValidationMode.valueOf(invoice.proofValidationMode())`.
5. `strategy = strategyResolver.resolve(mode)`.
6. Build `ProofValidationContext` from the command + invoice snapshot.
7. `status = strategy.resolveStatus(context)`.
8. `filePath = proofFileStorageGateway.store(new StoreProofFileCommand(...))` — file is
   written to disk **before** the DB row is created, so a storage failure never leaves a
   dangling `payment_proof` row pointing at a file that was never written.
9. Build `PaymentProof` via `createBuilder`, `requiresManualReview = status ==
   PENDING_MANUAL_REVIEW`.
10. `paymentProofPersistenceGateway.save(proof)`.
11. Return `UploadProofResult(proof.getId(), status)` (a small core-side result type —
    the entrypoint maps it to the HTTP response, §10).

No transaction spans steps 8–10 across the filesystem write — file I/O can't participate
in a JPA transaction. Accepted risk for this spec (self-hosted, single admin, low
volume): a crash between step 8 and step 10 leaves an orphaned file on disk with no DB
row, never the reverse (no DB row ever references a file that doesn't exist). No
automatic cleanup is proposed here — flagged as a deferred item (§13).

## 10. Entrypoint — response & controller changes

### 10.1 Response message per status

```java
private static String messageFor(final PaymentProofStatus status) {
    return switch (status) {
        case PENDING_ANALYSIS, PENDING_MANUAL_REVIEW ->
                "Your payment proof has been received and is being reviewed. "
                + "You will be notified of the result.";
        case APPROVED -> "Your payment has been approved. Thank you!";
        case REJECTED -> "Your payment proof was rejected. "
                + "Please contact the system administrator for more information.";
    };
}
```

Exhaustive `switch` over the enum — `APPROVED`/`REJECTED` are unreachable from `MANUAL`
today (only reachable once AI or a future synchronous manual-approval path exists), but
the switch handles the full enum now so nothing needs revisiting when that lands. Wording
deliberately doesn't say "manual" or "AI" anywhere — matches the requirement that the
debtor never sees which validation mode is behind the scenes.

### 10.2 `ProofUploadResponseDto`

```java
public record ProofUploadResponseDto(UUID proofId, String message) {}
```

Adds `proofId`, matching the `202` response shape already documented in
`BUSINESS_RULES.md` §7.3. Already registered in `PaymentAotConfig` — no AOT change
needed (same class, new field only).

### 10.3 `PublicProofController#uploadProof`

Shrinks to orchestration only — structural validation (`validateUpload`, unchanged) stays
in the controller (Layer 1, per the project's validation philosophy), business logic
moves to `UploadProofService`:

```java
@ResponseBody
@ResponseStatus(HttpStatus.ACCEPTED)
@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public ProofUploadResponseDto uploadProof(
        @RequestParam("token") final String token,
        @RequestParam("file") final MultipartFile file,
        @RequestParam("paymentType") final PaymentType paymentType,
        @RequestParam(value = "declaredAmount", required = false) final BigDecimal declaredAmount)
        throws IOException {

    final UUID parsedToken = parseTokenOrThrow(token);
    validateUpload(file, paymentType, declaredAmount);

    final var result = uploadProofUseCase.execute(new UploadProofCommand(
            parsedToken, file.getBytes(), file.getContentType(),
            file.getOriginalFilename(), paymentType, declaredAmount));

    return new ProofUploadResponseDto(result.proofId(), messageFor(result.status()));
}
```

The existing structured log line (invoiceId, paymentType, declaredAmount, filename,
content-type, size) moves into `UploadProofService` (it now has the invoice already
loaded) — same fields, same "never log the file's bytes" rule.

## 11. Infrastructure

### 11.1 `PaymentProofEntity` — updated

Adds `originalFilename`, `fileContentType`, `fileSizeBytes` fields (`@Column`, matching
§6.1); `status` field type changes from the infra-local enum to the relocated
`com.caimanproject.payment.core.domain.types.PaymentProofStatus` (§4).

### 11.2 `PaymentProofPersistenceAdapter` (new)

Implements `PaymentProofPersistenceGateway`. Maps `PaymentProof` ↔ `PaymentProofEntity`
via a MapStruct mapper (`PaymentProofMapper`, `entrypoint`/`infrastructure` convention —
`core` never uses MapStruct, per `AGENT.md`), using `restoreBuilder` for entity→domain
(via the existing `CaimanBuilderProvider` SPI) and plain field mapping for domain→entity.

### 11.3 `ProofFileStorageAdapter` (new)

Implements `ProofFileStorageGateway`:
1. Builds the filename per §3.3 (sanitization, extension-from-content-type, `UUID`,
   `UTC` timestamp).
2. `Files.write(storageRoot.resolve(filename), fileContent)`.
3. Returns the filename (relative path from the storage root — matches the existing
   doc comment on `payment_proof.file_path`).

`storageRoot` comes from `CaimanServerProps.PaymentProp.proofStorageFolderPath()` (§3.1).

### 11.4 `PaymentProofStorageInitializer` (new, see §3.2)

## 12. Docker / Compose

**No `Dockerfile.jvm` / `Dockerfile.native` changes.** Both already `mkdir -p /app/data`,
`chown` it to the `caiman` user, and declare `VOLUME ["/app/logs", "/app/data"]` —
proofs live in a subfolder of the directory that's already there with correct
permissions, regardless of DB backend.

- `docker-compose.sqlite.yml` — **no change**. Already mounts `caiman_data:/app/data`
  (currently just the sqlite file); set
  `CAIMAN_SERVER_PAYMENT_PROOF_STORAGE_FOLDER_PATH=/app/data/proofs` alongside it.
- `docker-compose.postgres.yml` — **needs a new named volume**. Today the
  `caiman-server` service only mounts `caiman_logs:/app/logs` (Postgres never touches
  app-local files, so nothing was needed there before). Add:
  ```yaml
  volumes:
    - caiman_logs:/app/logs
    - caiman_proof_storage:/app/data
  ```
  and a matching top-level `caiman_proof_storage: { name: caiman_proof_storage }`
  volume declaration. Set the same env var,
  `CAIMAN_SERVER_PAYMENT_PROOF_STORAGE_FOLDER_PATH=/app/data/proofs`.
- Without the postgres-compose volume, proof files would live in the container's
  writable layer and vanish on `docker compose down` / image update — the whole point
  of the change is to avoid exactly that.

**Operational note for the spec's audience (not a code change):** backups for the
Postgres deployment now need to cover the `caiman_proof_storage` volume in addition to
`pg_dump`/`caiman_db_data` — previously a `pg_dump` alone was a complete backup. Worth a
line in ops documentation whenever that gets written; not this spec's job to write it.

## 13. Risks / deferred items

- **Orphaned files on crash** (§9, step 8–10 gap): a process crash between writing the
  file and committing the DB row leaves an unreferenced file on disk. No cleanup job
  proposed here — low likelihood, low cost (disk space only, no correctness impact,
  self-hosted single-admin tool). A future janitor job (compare files under the storage
  root against `payment_proof.file_path` rows) could address this if it ever matters.
- **AI_AUTO / AI_ASSISTED implementation** — explicitly deferred, `// TODO`-marked
  stubs only (§7.2).
- **Payment registration, admin manual review resolution, payment-result notifications**
  (`BUSINESS_RULES.md` §9) — separate future spec, as noted in §2.
- **`token_expires_at` vestigial column** — inherited note from the prior spec (§13
  there), still unaddressed; `PaymentProof.tokenExpiresAt` is set to `Instant.MAX` as a
  placeholder in `createBuilder` (§6.3) since the column is `NOT NULL` and tokens don't
  actually expire. Whoever eventually removes the column also removes this workaround.
- **GraalVM native image** — `java.nio.file.Files` calls (mkdir, permission checks,
  write) involve no reflection and need no new hints. The one required native-image
  change is the `CaimanRuntimeHints` registration for `PaymentPropImpl` (§3.1) — a config
  record, not a filesystem concern, but easy to forget since it's a one-line addition in
  an unrelated file.

## 14. Testing

- **Unit** (`@UnitTest`):
  - `UploadProofService` — conflict-check matrix (active proof / paid / cancelled /
    none), strategy dispatch by mode, `PaymentProof` construction, result mapping.
  - `ManualProofValidationStrategy` — always returns `PENDING_MANUAL_REVIEW`.
  - `AiAutoProofValidationStrategy` / `AiAssistedProofValidationStrategy` — assert
    `UnsupportedOperationException`.
  - `ProofValidationStrategyResolver` — resolves each mode to the right bean; unknown
    mode throws.
  - `PaymentProof` — invariant validation (missing required fields → `DomainException`).
  - Filename generation (wherever it lives, likely a small pure function inside
    `ProofFileStorageAdapter` worth unit-testing directly) — sanitization of
    slashes/unicode/oversized names, correct extension per content-type.
- **Integration** (`@IntegrationTest`, run against both SQLite and Postgres per the
  project's existing dual-DB convention):
  - `POST` happy path (`MANUAL` mode) → `202`, `payment_proof` row created with
    `status = PENDING_MANUAL_REVIEW`, file present on disk at the recorded `file_path`,
    response message matches §10.1.
  - `POST` against a plan with `AI_AUTO`/`AI_ASSISTED` → `500` (unhandled
    `UnsupportedOperationException` surfaces through the global exception handler) — this
    is expected/acceptable for this spec; not a scenario to special-case.
  - `POST` with an active proof already pending → `422`, `ACTIVE_PROOF_EXISTS`.
  - `POST` against a `PAID`/`CANCELLED` invoice → `422`, `INVOICE_NOT_PAYABLE`.
  - `POST` with an unknown token → `404` (unchanged).
  - App fails to start when `CAIMAN_SERVER_PAYMENT_PROOF_STORAGE_FOLDER_PATH` points at
    a non-writable location (e.g. a read-only mount in the test) —
    `PaymentProofStorageInitializer` test, asserts context startup failure with a clear
    message.
