package com.caimanproject.billing.core.domain.model;

import com.caimanproject.billing.core.domain.types.DomainExceptionCode;
import com.caimanproject.billing.core.domain.types.InvoiceStatus;
import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.contracts.util.DomainValidation;
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
public class Invoice {

    @Getter(AccessLevel.NONE)
    private final UUID id;

    private final UUID chargePlanId;

    private final UUID chargePlanMemberId;

    private final Integer cycleIndex;

    private final BigDecimal amountDue;

    private final BigDecimal amountPaid;

    private final InvoiceStatus status;

    private final Instant dueDate;

    @Getter(AccessLevel.NONE)
    private final String cancellationReason;

    @Getter(AccessLevel.NONE)
    private final Instant cancelledAt;

    @Getter(AccessLevel.NONE)
    private final Instant paidAt;

    private final Audit audit;

    @Builder(builderMethodName = "restoreBuilder", builderClassName = "RestoreBuilder")
    public Invoice(
            final UUID id,
            final UUID chargePlanId,
            final UUID chargePlanMemberId,
            final Integer cycleIndex,
            final BigDecimal amountDue,
            final BigDecimal amountPaid,
            final InvoiceStatus status,
            final Instant dueDate,
            final String cancellationReason,
            final Instant cancelledAt,
            final Instant paidAt,
            final Audit audit) {

        // Optional
        this.id = id;
        this.cancellationReason = cancellationReason;
        this.cancelledAt = cancelledAt;
        this.paidAt = paidAt;

        // Required
        this.chargePlanId = chargePlanId;
        this.chargePlanMemberId = chargePlanMemberId;
        this.cycleIndex = cycleIndex;
        this.amountDue = amountDue;
        this.amountPaid = amountPaid;
        this.status = status;
        this.dueDate = dueDate;
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);

        final var fieldValidations = DomainValidation.validateAll(List.of(
                DomainValidation.validate(chargePlanId, "$.chargePlanId", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(
                        chargePlanMemberId, "$.chargePlanMemberId", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(cycleIndex, "$.cycleIndex", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(amountDue, "$.amountDue", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(amountPaid, "$.amountPaid", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(status, "$.status", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(dueDate, "$.dueDate", DomainExceptionCode.INVALID_VALUE)));

        fieldValidations.throwIfInvalid(DomainException::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public Invoice(
            final UUID chargePlanId,
            final UUID chargePlanMemberId,
            final Integer cycleIndex,
            final BigDecimal amountDue,
            final Instant dueDate) {
        this(
                null,
                chargePlanId,
                chargePlanMemberId,
                cycleIndex,
                amountDue,
                BigDecimal.ZERO,
                InvoiceStatus.PENDING,
                dueDate,
                null,
                null,
                null,
                null);
    }

    public Optional<UUID> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<String> getCancellationReason() {
        return Optional.ofNullable(cancellationReason);
    }

    public Optional<Instant> getCancelledAt() {
        return Optional.ofNullable(cancelledAt);
    }

    public Optional<Instant> getPaidAt() {
        return Optional.ofNullable(paidAt);
    }
}
