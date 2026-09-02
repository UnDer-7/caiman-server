package com.caimanproject.notification.core.domain.model;

import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.contracts.util.DomainValidation;
import com.caimanproject.notification.core.domain.types.DomainExceptionCode;
import com.caimanproject.notification.core.domain.types.NotificationChannel;
import com.caimanproject.notification.core.domain.types.NotificationOutboxStatus;
import com.caimanproject.notification.core.domain.types.TriggerType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class NotificationOutbox {

    @Getter(AccessLevel.NONE)
    private final UUID id;

    private final UUID invoiceId;

    private final TriggerType triggerType;

    private final NotificationChannel channel;

    private final String recipient;

    private final String debtorName;

    private final String planName;

    private final BigDecimal amountDue;

    @Getter(AccessLevel.NONE)
    private final Instant dueDate;

    @Getter(AccessLevel.NONE)
    private final String uploadLink;

    @Getter(AccessLevel.NONE)
    private final Long cycleIndex;

    @Getter(AccessLevel.NONE)
    private final String rejectionReason;

    private final Instant scheduledFor;

    private final NotificationOutboxStatus status;

    private final Integer attemptCount;

    private final Integer maxAttempts;

    @Getter(AccessLevel.NONE)
    private final Instant lastAttemptedAt;

    @Getter(AccessLevel.NONE)
    private final String lastError;

    private final Audit audit;

    @Builder(builderMethodName = "restoreBuilder", builderClassName = "RestoreBuilder")
    public NotificationOutbox(
            final UUID id,
            final UUID invoiceId,
            final TriggerType triggerType,
            final NotificationChannel channel,
            final String recipient,
            final String debtorName,
            final String planName,
            final BigDecimal amountDue,
            final Instant dueDate,
            final String uploadLink,
            final Long cycleIndex,
            final String rejectionReason,
            final Instant scheduledFor,
            final NotificationOutboxStatus status,
            final Integer attemptCount,
            final Integer maxAttempts,
            final Instant lastAttemptedAt,
            final String lastError,
            final Audit audit) {

        // Optional
        this.id = id;
        this.dueDate = dueDate;
        this.uploadLink = uploadLink;
        this.cycleIndex = cycleIndex;
        this.rejectionReason = rejectionReason;
        this.lastAttemptedAt = lastAttemptedAt;
        this.lastError = lastError;

        // Required
        this.invoiceId = invoiceId;
        this.triggerType = triggerType;
        this.channel = channel;
        this.recipient = recipient;
        this.debtorName = debtorName;
        this.planName = planName;
        this.amountDue = amountDue;
        this.scheduledFor = scheduledFor;
        this.status = status;
        this.attemptCount = attemptCount;
        this.maxAttempts = maxAttempts;
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);

        final var fieldValidations = DomainValidation.validateAll(List.of(
                DomainValidation.validate(invoiceId, "$.invoiceId", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(triggerType, "$.triggerType", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(channel, "$.channel", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(recipient, "$.recipient", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(debtorName, "$.debtorName", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(planName, "$.planName", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(amountDue, "$.amountDue", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(scheduledFor, "$.scheduledFor", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(status, "$.status", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(attemptCount, "$.attemptCount", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(maxAttempts, "$.maxAttempts", DomainExceptionCode.INVALID_VALUE)));

        fieldValidations.throwIfInvalid(DomainException::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public NotificationOutbox(
            final UUID invoiceId,
            final TriggerType triggerType,
            final NotificationChannel channel,
            final String recipient,
            final String debtorName,
            final String planName,
            final BigDecimal amountDue,
            final Instant dueDate,
            final String uploadLink,
            final Long cycleIndex,
            final String rejectionReason,
            final Instant scheduledFor,
            final Integer maxAttempts) {
        this(
                null,
                invoiceId,
                triggerType,
                channel,
                recipient,
                debtorName,
                planName,
                amountDue,
                dueDate,
                uploadLink,
                cycleIndex,
                rejectionReason,
                scheduledFor,
                NotificationOutboxStatus.SCHEDULED,
                0,
                maxAttempts,
                null,
                null,
                null);
    }

    public boolean isScheduled() {
        return status == NotificationOutboxStatus.SCHEDULED;
    }

    public boolean isProcessing() {
        return status == NotificationOutboxStatus.PROCESSING;
    }

    public boolean isRetryScheduled() {
        return status == NotificationOutboxStatus.RETRY_SCHEDULED;
    }

    public NotificationOutbox markProcessing(final Instant now) {
        return toRestoreBuilder()
                .status(NotificationOutboxStatus.PROCESSING)
                .lastAttemptedAt(now)
                .build();
    }

    public NotificationOutbox markFailedAndReschedule(
            final Instant nextRetry, final Instant attemptedAt, final String errorMessage) {
        return toRestoreBuilder()
                .status(NotificationOutboxStatus.RETRY_SCHEDULED)
                .attemptCount(this.attemptCount + 1)
                .scheduledFor(nextRetry)
                .lastAttemptedAt(attemptedAt)
                .lastError(errorMessage)
                .build();
    }

    public NotificationOutbox markStuckReset() {
        return toRestoreBuilder()
                .status(NotificationOutboxStatus.SCHEDULED)
                .attemptCount(this.attemptCount + 1)
                .lastError("Reset from stuck PROCESSING state")
                .build();
    }

    public boolean isExhausted() {
        return this.attemptCount >= this.maxAttempts;
    }

    private RestoreBuilder toRestoreBuilder() {
        return NotificationOutbox.restoreBuilder()
                .id(id)
                .invoiceId(invoiceId)
                .triggerType(triggerType)
                .channel(channel)
                .recipient(recipient)
                .debtorName(debtorName)
                .planName(planName)
                .amountDue(amountDue)
                .dueDate(dueDate)
                .uploadLink(uploadLink)
                .cycleIndex(cycleIndex)
                .rejectionReason(rejectionReason)
                .scheduledFor(scheduledFor)
                .status(status)
                .attemptCount(attemptCount)
                .maxAttempts(maxAttempts)
                .lastAttemptedAt(lastAttemptedAt)
                .lastError(lastError)
                .audit(audit);
    }

    public Optional<UUID> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<Instant> getDueDate() {
        return Optional.ofNullable(dueDate);
    }

    public Optional<String> getUploadLink() {
        return Optional.ofNullable(uploadLink);
    }

    public Optional<Long> getCycleIndex() {
        return Optional.ofNullable(cycleIndex);
    }

    public Optional<String> getRejectionReason() {
        return Optional.ofNullable(rejectionReason);
    }

    public Optional<Instant> getLastAttemptedAt() {
        return Optional.ofNullable(lastAttemptedAt);
    }

    public Optional<String> getLastError() {
        return Optional.ofNullable(lastError);
    }
}
