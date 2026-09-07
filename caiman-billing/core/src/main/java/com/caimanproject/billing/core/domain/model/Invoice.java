package com.caimanproject.billing.core.domain.model;

import com.caimanproject.billing.core.domain.types.DomainExceptionCode;
import com.caimanproject.billing.core.domain.types.InvoiceStatus;
import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.contracts.util.DomainValidation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

    private final Long cycleIndex;

    private final LocalDate generationDate;

    private final BigDecimal amountDue;

    private final BigDecimal amountPaid;

    private final InvoiceStatus status;

    private final Instant dueDate;

    private final UUID uploadToken;

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
            final Long cycleIndex,
            final LocalDate generationDate,
            final BigDecimal amountDue,
            final BigDecimal amountPaid,
            final InvoiceStatus status,
            final Instant dueDate,
            final UUID uploadToken,
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
        this.generationDate = generationDate;
        this.amountDue = amountDue;
        this.amountPaid = amountPaid;
        this.status = status;
        this.dueDate = dueDate;
        this.uploadToken = uploadToken;
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);

        final var fieldValidations = DomainValidation.validateAll(List.of(
                DomainValidation.validate(chargePlanId, "$.chargePlanId", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(
                        chargePlanMemberId, "$.chargePlanMemberId", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(cycleIndex, "$.cycleIndex", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(generationDate, "$.generationDate", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(amountDue, "$.amountDue", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(amountPaid, "$.amountPaid", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(status, "$.status", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(dueDate, "$.dueDate", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(uploadToken, "$.uploadToken", DomainExceptionCode.INVALID_VALUE)));

        fieldValidations.throwIfInvalid(DomainException::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public Invoice(
            final UUID chargePlanId,
            final UUID chargePlanMemberId,
            final Long cycleIndex,
            final LocalDate generationDate,
            final BigDecimal amountDue,
            final Instant dueDate) {
        this(
                null,
                chargePlanId,
                chargePlanMemberId,
                cycleIndex,
                generationDate,
                amountDue,
                BigDecimal.ZERO,
                isFullyCoveredByCredit(amountDue) ? InvoiceStatus.PAID : InvoiceStatus.PENDING,
                dueDate,
                UUID.randomUUID(),
                null,
                null,
                isFullyCoveredByCredit(amountDue) ? Instant.now() : null,
                null);
    }

    private static boolean isFullyCoveredByCredit(final BigDecimal amountDue) {
        return amountDue != null && amountDue.compareTo(BigDecimal.ZERO) == 0;
    }

    public Invoice markSent() {
        if (status != InvoiceStatus.PENDING) {
            return this;
        }
        return toRestoreBuilder().status(InvoiceStatus.SENT).build();
    }

    private RestoreBuilder toRestoreBuilder() {
        return Invoice.restoreBuilder()
                .id(id)
                .chargePlanId(chargePlanId)
                .chargePlanMemberId(chargePlanMemberId)
                .cycleIndex(cycleIndex)
                .generationDate(generationDate)
                .amountDue(amountDue)
                .amountPaid(amountPaid)
                .status(status)
                .dueDate(dueDate)
                .uploadToken(uploadToken)
                .cancellationReason(cancellationReason)
                .cancelledAt(cancelledAt)
                .paidAt(paidAt)
                .audit(audit);
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
