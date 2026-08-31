# Huginn Job — Email Dispatch Design

**Date:** 2026-08-31
**Status:** Approved for implementation planning
**Bounded context:** `caiman-notification`

## 1. Goal

Implement the actual dispatch logic behind `HuginnJobStarter` / `RunHuginnUseCase`, currently a stub. Huginn must:

1. Read eligible `notification_outbox` rows in a semi-paginated loop (fixed-size batches, re-query until a batch comes back empty).
2. Dispatch each row through a per-channel strategy (only `EMAIL` implemented; the strategy itself is a mock — no real SMTP yet).
3. Retry failed rows (exponential backoff, capped, bounded by `max_attempts`) using the same loop and the same strategy.
4. Record every attempt in `notification_log`.

This spec covers only the dispatch pipeline. It does not implement the invoice-status transition on successful `INVOICE_CREATED` dispatch (§11.3 of `BUSINESS_RULES.md`) — that requires an `InvoiceGateway` that does not exist yet and is intentionally deferred. `RunHuginnService` gets a `TODO` comment marking the gap.

## 2. Non-goals (explicit)

- No real SMTP integration. `EmailNotificationSenderAdapter.send()` logs `"Sending email MOCK"` and returns `true`.
- No `InvoiceGateway` / invoice status mutation. Marked with a `TODO` comment in `RunHuginnService`.
- No admin resend-link flow (§12.1), no `LINK_RESENT` trigger type.
- No WhatsApp/Telegram strategy — only the `NotificationSenderGateway` abstraction plus its single `EMAIL` implementation.
- No env-var-driven batch size. Hardcoded constant for now (see §6).
- Detailed per-exception error messages: `NotificationSenderGateway.send()` returns only a `boolean` (explicit product decision — see §7 limitation).

## 3. Business rules touched — `BUSINESS_RULES.md` §11 amended

While writing this spec, a contradiction was found and resolved directly in `docs/BUSINESS_RULES.md`:

- §11.1's pseudocode sent a failed-but-retryable row back to `status = 'SCHEDULED'`.
- §11.2's status table described `FAILED` as "will be retried, `attempt_count < max_attempts`" — implying `FAILED` should be a *persisted* outbox status, which the §11.1 pseudocode never actually produced.
- Appendix A.3 fixed the valid outbox status set at exactly three values, but under the old `FAILED` name this was ambiguous: a reader couldn't tell from the status alone whether a `FAILED` row was retryable or terminal.

**Resolution (implemented in the docs, not just here):** introduced a fourth-in-name-only status, `RETRY_SCHEDULED`, replacing the old `FAILED` outbox status. A persisted `RETRY_SCHEDULED` row always means "will be retried" — there is no ambiguity, because exhausted rows are deleted immediately rather than left in any status. `notification_log.status = FAILED` is unrelated and unchanged — it is the per-attempt audit record, not an outbox state.

**Final valid outbox statuses:** `SCHEDULED`, `PROCESSING`, `RETRY_SCHEDULED`.

**Dispatch eligibility query:** `status IN ('SCHEDULED', 'RETRY_SCHEDULED') AND scheduled_for <= :now`, ordered by `scheduled_for ASC`. This is what lets a single loop handle both fresh sends and retries — the retry/non-retry distinction lives entirely in `attempt_count`/`last_error`, never in a separate code path; the strategy itself is retry-agnostic (returns a boolean either way), exactly as originally proposed.

Docs already edited: `docs/BUSINESS_RULES.md` §11.1 (query + retry pseudocode), §11.2 (status table), the enum quick-reference table (§ "quick reference" glossary), and Appendix A.3.

## 4. Domain model changes — `caiman-notification:core`

### 4.1 `NotificationOutboxStatus` (enum)

```java
public enum NotificationOutboxStatus {
    SCHEDULED,
    PROCESSING,
    RETRY_SCHEDULED
}
```

`FAILED` is removed (it never actually represented a reachable, correctly-modeled outbox state — see §3).

### 4.2 `NotificationOutbox` (domain model)

Rename `isFailed()` → `isRetryScheduled()` (checks `status == RETRY_SCHEDULED`; only current caller was the class itself, no external usages found).

Add state-transition methods, following the existing `restoreBuilder`-rebuild idiom used by `ChargePlan.withUpdatedMember`:

```java
public NotificationOutbox markProcessing(Instant now) {
    return this.toRestoreBuilder()
            .status(NotificationOutboxStatus.PROCESSING)
            .lastAttemptedAt(now)
            .build();
}

public NotificationOutbox markFailedAndReschedule(Instant nextRetry, Instant attemptedAt, String errorMessage) {
    return this.toRestoreBuilder()
            .status(NotificationOutboxStatus.RETRY_SCHEDULED)
            .attemptCount(this.attemptCount + 1)
            .scheduledFor(nextRetry)
            .lastAttemptedAt(attemptedAt)
            .lastError(errorMessage)
            .build();
}

public NotificationOutbox markStuckReset() {
    return this.toRestoreBuilder()
            .status(NotificationOutboxStatus.SCHEDULED)
            .attemptCount(this.attemptCount + 1)
            .lastError("Reset from stuck PROCESSING state")
            .build();
}

public boolean isExhausted() {
    return this.attemptCount >= this.maxAttempts;
}
```

`toRestoreBuilder()` is a small private helper that populates a fresh `restoreBuilder()` with every current field (same manual-rebuild pattern as `ChargePlan.withUpdatedMember`, just factored out once since `NotificationOutbox` needs it three times).

**Deviation from the literal §11.1 pseudocode, made deliberately:** `markProcessing` sets `lastAttemptedAt = now` even on a fresh (non-retry) attempt. The doc's pseudocode only sets `last_attempted_at` on failure. Without setting it when entering `PROCESSING`, a crash on a row's *first-ever* attempt would leave `last_attempted_at` `null`, and the §11.4 stuck-row query (`last_attempted_at < NOW() - 5 minutes`) would never match it — the row would be stuck in `PROCESSING` forever. Setting it on every transition into `PROCESSING` closes that gap. This does not change any business-visible behavior described in the docs.

## 5. Ports — `caiman-notification:core`

### 5.1 `NotificationSenderGateway` (new port.out — the Strategy)

```java
package com.caimanproject.notification.core.port.out;

import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import com.caimanproject.notification.core.domain.types.NotificationChannel;

public interface NotificationSenderGateway {

    NotificationChannel supportedChannel();

    boolean send(NotificationOutbox outbox);
}
```

One Spring bean per channel. `DispatchNotificationService` receives `List<NotificationSenderGateway>` and builds a `Map<NotificationChannel, NotificationSenderGateway>` in its constructor — standard Strategy resolution, same shape as `RunOdinService` dispatching to `RotatingInvoiceGenerator` / `SplitInvoiceGenerator` by `ChargePlan.type`.

### 5.2 `NotificationLogPersistenceGateway` (new port.out)

Per your instruction: the gateway receives the `NotificationOutbox` itself; the adapter is responsible for building the `NotificationLog` internally, not the core service.

```java
package com.caimanproject.notification.core.port.out;

import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import java.time.Instant;

public interface NotificationLogPersistenceGateway {

    void logSent(NotificationOutbox outbox, Instant sentAt);

    void logFailed(NotificationOutbox outbox, String errorMessage, Instant attemptedAt);
}
```

### 5.3 `NotificationOutboxPersistenceGateway` (extended)

```java
package com.caimanproject.notification.core.port.out;

import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationOutboxPersistenceGateway {

    NotificationOutbox save(NotificationOutbox outbox);

    List<NotificationOutbox> findEligibleForDispatch(Instant now, int limit);

    List<NotificationOutbox> findStuckProcessing(Instant threshold);

    void delete(UUID id);
}
```

## 6. Core services

### 6.1 `RunHuginnService` (implements existing `RunHuginnUseCase`)

Pure orchestrator — no dispatch logic of its own.

```java
private static final int BATCH_SIZE = 50; // BUSINESS_RULES.md §11.1 example batch size
private static final Duration STUCK_PROCESSING_THRESHOLD = Duration.ofMinutes(5); // §11.4

@Override
public void execute() {
    final Instant now = Instant.now(); // captured once for the whole run — never re-read mid-run

    resetStuckProcessing(now);

    List<NotificationOutbox> batch;
    do {
        batch = notificationOutboxPersistenceGateway.findEligibleForDispatch(now, BATCH_SIZE);
        batch.forEach(entry -> dispatchNotificationService.dispatch(entry, now));
        // TODO: notify invoice sent status
    } while (!batch.isEmpty());
}

private void resetStuckProcessing(final Instant now) {
    final Instant threshold = now.minus(STUCK_PROCESSING_THRESHOLD);
    notificationOutboxPersistenceGateway.findStuckProcessing(threshold).forEach(entry -> {
        final var reset = entry.markStuckReset();
        if (reset.isExhausted()) {
            notificationLogPersistenceGateway.logFailed(reset, "Reset from stuck PROCESSING state", now);
            notificationOutboxPersistenceGateway.delete(reset.getId().orElseThrow());
        } else {
            notificationOutboxPersistenceGateway.save(reset);
        }
    });
}
```

`BATCH_SIZE` is hardcoded, matching the existing `DEFAULT_MAX_ATTEMPTS` constant pattern in `RotatingInvoiceGenerator` / `SplitInvoiceGenerator` — no env var for now, per your call. Easy to promote to a config value later without touching the algorithm.

### 6.2 `DispatchNotificationService` (new — internal to `core`, not a `port.in`)

Handles exactly one outbox row. Not exposed as a use case interface since it's only ever called by `RunHuginnService` within the same module.

```java
package com.caimanproject.notification.core.domain.service;

@Service
@RequiredArgsConstructor
public class DispatchNotificationService {

    private final Map<NotificationChannel, NotificationSenderGateway> sendersByChannel; // built in a @PostConstruct or a constructor that consumes List<NotificationSenderGateway>
    private final NotificationOutboxPersistenceGateway outboxGateway;
    private final NotificationLogPersistenceGateway logGateway;

    public void dispatch(NotificationOutbox outbox, Instant now) {
        final var processing = outboxGateway.save(outbox.markProcessing(now));

        final NotificationSenderGateway sender = sendersByChannel.get(processing.getChannel());
        if (sender == null) {
            log.warn("no dispatcher implemented for channel {}, skipping", processing.getChannel());
            return; // leaves the row PROCESSING — will be picked up by the stuck-row reset next run
        }

        final boolean success = sender.send(processing);

        if (success) {
            logGateway.logSent(processing, now);
            outboxGateway.delete(processing.getId().orElseThrow());
            return;
        }

        final String errorMessage = "Notification dispatch failed";
        final Instant nextRetry = now.plus(nextBackoff(processing.getAttemptCount()));
        // markFailedAndReschedule always increments attemptCount first — isExhausted() is then
        // checked against the POST-increment count, so "this attempt" is the one that counts
        // toward max_attempts. nextRetry is computed unconditionally; it's simply discarded
        // below if the row turns out to be exhausted.
        final var attempted = processing.markFailedAndReschedule(nextRetry, now, errorMessage);

        if (attempted.isExhausted()) {
            logGateway.logFailed(attempted, errorMessage, now);
            outboxGateway.delete(attempted.getId().orElseThrow());
        } else {
            outboxGateway.save(attempted);
            logGateway.logFailed(attempted, errorMessage, now);
        }
    }

    private static Duration nextBackoff(int attemptCountBeforeThisFailure) {
        final long minutes = Math.min(1L << (attemptCountBeforeThisFailure + 1), 60L); // 2^attempt, capped at 60
        return Duration.ofMinutes(minutes);
    }
}
```

**Resolved contract:** `isExhausted()` always reads `attemptCount` as currently stored on the instance it's called on. The one rule that removes all ambiguity: every `markXxx` method that increments `attemptCount` (`markFailedAndReschedule`, `markStuckReset`) does so *before* returning, so `isExhausted()` is always called on the post-increment instance — never on the pre-attempt one. `DispatchNotificationService` and `RunHuginnService.resetStuckProcessing` both follow this same order (transition first, check `isExhausted()` on the result, branch to delete-vs-persist).

## 7. Known limitation — boolean-only strategy return

`NotificationSenderGateway.send()` returns only a `boolean` (explicit decision, since the strategy must not care about retry-vs-fresh distinction). Consequence: `notification_log.error_message` and `notification_outbox.last_error` get a fixed generic string (`"Notification dispatch failed"`) on every failure — no exception detail, no SMTP error code. Acceptable for this iteration since the only implementation is a mock. If/when the real SMTP adapter is built, revisit whether `NotificationSenderGateway` needs a richer return type (e.g. a small `SendResult(boolean success, String errorMessage)` record) instead of a bare `boolean` — flagged here so it isn't forgotten, not decided now.

## 8. Infrastructure — `caiman-notification:infrastructure`

### 8.1 `EmailNotificationSenderAdapter` (new)

New sub-package `infrastructure.email.adapter` (email sending doesn't fit the existing `database.adapter` / `messaging.adapter` / `http.adapter` categories from `HEXAGONAL_ARCHITECTURE.md` — `messaging.adapter` is documented as "event/message producers," i.e. domain event publishing, not literal email transport).

```java
package com.caimanproject.notification.infrastructure.email.adapter;

@Slf4j
@Component
public class EmailNotificationSenderAdapter implements NotificationSenderGateway {

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean send(NotificationOutbox outbox) {
        log.info("Sending email MOCK — recipient={}, triggerType={}", LogMask.email(outbox.getRecipient()), outbox.getTriggerType());
        return true;
    }
}
```

### 8.2 `NotificationLogPersistenceAdapter` (new)

Builds the `NotificationLog` domain object from the given `NotificationOutbox` internally (per your instruction), then persists it.

```java
package com.caimanproject.notification.infrastructure.database.adapter;

@Component
@RequiredArgsConstructor
public class NotificationLogPersistenceAdapter implements NotificationLogPersistenceGateway {

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationLogEntityMapper notificationLogEntityMapper;

    @Override
    @Transactional
    public void logSent(NotificationOutbox outbox, Instant sentAt) {
        save(outbox, NotificationLogStatus.SENT, null, sentAt);
    }

    @Override
    @Transactional
    public void logFailed(NotificationOutbox outbox, String errorMessage, Instant attemptedAt) {
        save(outbox, NotificationLogStatus.FAILED, errorMessage, attemptedAt);
    }

    private void save(NotificationOutbox outbox, NotificationLogStatus status, String errorMessage, Instant sentAt) {
        final NotificationLog log = NotificationLog.createBuilder()
                .invoiceId(outbox.getInvoiceId())
                .outboxId(outbox.getId().orElseThrow())
                .triggerType(outbox.getTriggerType())
                .channel(outbox.getChannel())
                .status(status)
                .recipient(outbox.getRecipient())
                .errorMessage(errorMessage)
                .sentAt(sentAt)
                .build();
        notificationLogRepository.save(notificationLogEntityMapper.toEntity(log));
    }
}
```

### 8.3 New supporting infra files

- `NotificationLogRepository` (new) — `CrudRepository<NotificationLogEntity, String>`, no custom queries needed (append-only, write-then-forget).
- `NotificationLogEntityMapper` (new) — MapStruct, same shape as `NotificationOutboxEntityMapper` (`uses = {OptionalMapper.class, IdMapper.class, NotificationAuditEntityMapper.class}`).

### 8.4 `NotificationOutboxRepository` — new queries

```java
@Query("""
    SELECT n FROM NotificationOutboxEntity n
    WHERE n.status IN (com.caimanproject.notification.core.domain.types.NotificationOutboxStatus.SCHEDULED,
                        com.caimanproject.notification.core.domain.types.NotificationOutboxStatus.RETRY_SCHEDULED)
      AND n.scheduledFor <= :now
    ORDER BY n.scheduledFor ASC
    """)
List<NotificationOutboxEntity> findEligibleForDispatch(@Param("now") Instant now, Pageable pageable);

@Query("""
    SELECT n FROM NotificationOutboxEntity n
    WHERE n.status = com.caimanproject.notification.core.domain.types.NotificationOutboxStatus.PROCESSING
      AND n.lastAttemptedAt < :threshold
    """)
List<NotificationOutboxEntity> findStuckProcessing(@Param("threshold") Instant threshold);
```

(Exact enum-literal-in-JPQL syntax to be confirmed against the Hibernate version at implementation time — a named-parameter bind of the enum value is an equally valid alternative if the literal form proves awkward; either way, no native/dialect-specific SQL, so this stays portable between SQLite and Postgres per the project's dual-DB requirement.)

`NotificationOutboxPersistenceAdapter` gets two new pass-through methods wrapping these queries with `PageRequest.of(0, limit)`, plus a `delete(UUID id)` wrapping `notificationOutboxRepository.deleteById(id.toString())`.

## 9. `caiman-notification:entrypoint`

No changes. `HuginnJobStarter` keeps its existing `// todo: ver como lidar com erros e o retry do job` comment as-is — error handling and retry now live entirely in `RunHuginnService`/`DispatchNotificationService`, but the comment stays per your instruction.

## 10. File change summary

**New files:**
- `core/.../domain/service/DispatchNotificationService.java`
- `core/.../port/out/NotificationSenderGateway.java`
- `core/.../port/out/NotificationLogPersistenceGateway.java`
- `infrastructure/.../email/adapter/EmailNotificationSenderAdapter.java`
- `infrastructure/.../database/adapter/NotificationLogPersistenceAdapter.java`
- `infrastructure/.../database/repository/NotificationLogRepository.java`
- `infrastructure/.../database/mapper/NotificationLogEntityMapper.java`

**Modified files:**
- `core/.../domain/types/NotificationOutboxStatus.java` — `FAILED` → `RETRY_SCHEDULED`
- `core/.../domain/model/NotificationOutbox.java` — rename `isFailed()` → `isRetryScheduled()`; add `markProcessing`, `markFailedAndReschedule`, `markStuckReset`, `isExhausted`, `toRestoreBuilder` helper
- `core/.../domain/service/RunHuginnService.java` — real implementation (was a logging stub)
- `core/.../port/out/NotificationOutboxPersistenceGateway.java` — add `findEligibleForDispatch`, `findStuckProcessing`, `delete`
- `infrastructure/.../database/adapter/NotificationOutboxPersistenceAdapter.java` — implement the three new gateway methods
- `infrastructure/.../database/repository/NotificationOutboxRepository.java` — add the two new `@Query` methods
- `docs/BUSINESS_RULES.md` — already applied (§11.1, §11.2, glossary table, Appendix A.3)

**Untouched (explicitly):**
- `entrypoint/.../job/HuginnJobStarter.java`
- Everything invoice-related (`InvoiceGateway` not created)
- `NotificationLogStatus` enum (`SENT`, `FAILED` — unchanged, correct as-is)

## 11. Testing notes (for the implementation plan, not executed here)

- `NotificationOutbox` transition methods: unit-testable in isolation (pure domain, no I/O) — verify each `markXxx` produces the expected field set and leaves the original instance unchanged (immutability).
- `DispatchNotificationService`: unit test with mocked `NotificationSenderGateway`/gateways — cover success, retryable failure, exhausted failure, and unknown-channel paths.
- `RunHuginnService`: unit test the loop termination (batch returns empty → stops) and the stuck-reset pre-pass, with a mocked outbox gateway.
- Repository queries (`findEligibleForDispatch`, `findStuckProcessing`): integration test against both SQLite and Postgres per the project's dual-DB testing setup, since these are hand-written JPQL.
