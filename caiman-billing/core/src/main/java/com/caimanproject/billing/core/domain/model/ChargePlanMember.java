package com.caimanproject.billing.core.domain.model;

import com.caimanproject.billing.core.domain.types.ChargePlanMemberStatus;
import com.caimanproject.billing.core.domain.types.DomainExceptionCode;
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
public class ChargePlanMember {

    @Getter(AccessLevel.NONE)
    private final UUID id;

    private final UUID debtorId;

    @Getter(AccessLevel.NONE)
    private final BigDecimal amountOverride;

    @Getter(AccessLevel.NONE)
    private final Integer rotationOrder;

    private final ChargePlanMemberStatus status;

    private final BigDecimal creditBalance;

    private final Instant joinedAt;

    @Getter(AccessLevel.NONE)
    private final Instant leftAt;

    private final Audit audit;

    @Builder(builderMethodName = "restoreBuilder", builderClassName = "RestoreBuilder")
    public ChargePlanMember(
            final UUID id,
            final UUID debtorId,
            final BigDecimal amountOverride,
            final Integer rotationOrder,
            final ChargePlanMemberStatus status,
            final BigDecimal creditBalance,
            final Instant joinedAt,
            final Instant leftAt,
            final Audit audit) {

        // Optional
        this.id = id;
        this.amountOverride = amountOverride;
        this.rotationOrder = rotationOrder;
        this.leftAt = leftAt;

        // Required
        this.debtorId = debtorId;
        this.status = status;
        this.creditBalance = creditBalance;
        this.joinedAt = joinedAt;
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);

        final var fieldValidation = DomainValidation.validateAll(List.of(
                DomainValidation.validate(debtorId, "$.debtorId", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(status, "$.status", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(creditBalance, "$.creditBalance", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(joinedAt, "$.joinedAt", DomainExceptionCode.INVALID_VALUE)));

        fieldValidation.throwIfInvalid(DomainException::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public ChargePlanMember(
            final UUID debtorId,
            final BigDecimal amountOverride,
            final Integer rotationOrder,
            final BigDecimal creditBalance,
            final Instant leftAt) {
        this(
                null,
                debtorId,
                amountOverride,
                rotationOrder,
                ChargePlanMemberStatus.ACTIVE,
                creditBalance,
                Instant.now(),
                leftAt,
                null);
    }

    public boolean isActive() {
        return getStatus() == ChargePlanMemberStatus.ACTIVE;
    }

    public Charge chargeForCycle(final BigDecimal chargePlanTotalAmount) {
        final var baseAmount = getAmountOverride()
                .filter(override -> override.compareTo(BigDecimal.ZERO) > 0)
                .orElse(chargePlanTotalAmount);

        final var amountDue = baseAmount.subtract(creditBalance).max(BigDecimal.ZERO);
        final var remainingCreditBalance = creditBalance.subtract(baseAmount).max(BigDecimal.ZERO);

        return new Charge(amountDue, withCreditBalance(remainingCreditBalance));
    }

    private ChargePlanMember withCreditBalance(final BigDecimal newCreditBalance) {
        return ChargePlanMember.restoreBuilder()
                .id(id)
                .debtorId(debtorId)
                .amountOverride(amountOverride)
                .rotationOrder(rotationOrder)
                .status(status)
                .creditBalance(newCreditBalance)
                .joinedAt(joinedAt)
                .leftAt(leftAt)
                .audit(audit)
                .build();
    }

    public record Charge(BigDecimal amountDue, ChargePlanMember updatedMember) {}

    public Optional<UUID> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<BigDecimal> getAmountOverride() {
        return Optional.ofNullable(amountOverride);
    }

    public Optional<Integer> getRotationOrder() {
        return Optional.ofNullable(rotationOrder);
    }

    public Optional<Instant> getLeftAt() {
        return Optional.ofNullable(leftAt);
    }
}
