package com.caimanproject.notification.core.domain.model;

import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.contracts.util.DomainValidation;
import com.caimanproject.notification.core.domain.types.DomainExceptionCode;
import com.caimanproject.notification.core.domain.types.NotificationChannel;
import com.caimanproject.notification.core.domain.types.NotificationLogStatus;
import com.caimanproject.notification.core.domain.types.TriggerType;
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
public class NotificationLog {

    @Getter(AccessLevel.NONE)
    private final UUID id;

    private final UUID invoiceId;

    private final UUID outboxId;

    private final TriggerType triggerType;

    private final NotificationChannel channel;

    private final NotificationLogStatus status;

    private final String recipient;

    @Getter(AccessLevel.NONE)
    private final String errorMessage;

    private final Instant sentAt;

    private final Audit audit;

    @Builder(builderMethodName = "restoreBuilder", builderClassName = "RestoreBuilder")
    public NotificationLog(
            final UUID id,
            final UUID invoiceId,
            final UUID outboxId,
            final TriggerType triggerType,
            final NotificationChannel channel,
            final NotificationLogStatus status,
            final String recipient,
            final String errorMessage,
            final Instant sentAt,
            final Audit audit) {

        // Optional
        this.id = id;
        this.errorMessage = errorMessage;

        // Required
        this.invoiceId = invoiceId;
        this.outboxId = outboxId;
        this.triggerType = triggerType;
        this.channel = channel;
        this.status = status;
        this.recipient = recipient;
        this.sentAt = sentAt;
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);

        final var fieldValidations = DomainValidation.validateAll(List.of(
                DomainValidation.validate(invoiceId, "$.invoiceId", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(outboxId, "$.outboxId", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(triggerType, "$.triggerType", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(channel, "$.channel", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(status, "$.status", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(recipient, "$.recipient", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(sentAt, "$.sentAt", DomainExceptionCode.INVALID_VALUE)));

        fieldValidations.throwIfInvalid(DomainException::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public NotificationLog(
            final UUID invoiceId,
            final UUID outboxId,
            final TriggerType triggerType,
            final NotificationChannel channel,
            final NotificationLogStatus status,
            final String recipient,
            final String errorMessage,
            final Instant sentAt) {
        this(null, invoiceId, outboxId, triggerType, channel, status, recipient, errorMessage, sentAt, null);
    }

    public boolean isSent() {
        return status == NotificationLogStatus.SENT;
    }

    public boolean isFailed() {
        return status == NotificationLogStatus.FAILED;
    }

    public Optional<UUID> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
}
