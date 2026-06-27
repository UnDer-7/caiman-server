package com.caimanproject.billing.core.domain.model;

import com.caimanproject.billing.core.domain.exception.domain.DomainExceptionCode;
import com.caimanproject.billing.core.domain.types.ChargePlanMemberStatus;
import com.caimanproject.contracts.util.DomainValidation;
import java.math.BigDecimal;
import java.time.Instant;
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

    private final ChargePlan chargePlan;

    private final String debtorId;

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
            final ChargePlan chargePlan,
            final String debtorId,
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
        this.chargePlan = validateOrThrows(chargePlan, "chargePlan");
        this.debtorId = validateOrThrows(debtorId, "debtorId");
        this.status = validateOrThrows(status, "status");
        this.creditBalance = validateOrThrows(creditBalance, "creditBalance");
        this.joinedAt = validateOrThrows(joinedAt, "joinedAt");
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public ChargePlanMember(
            final ChargePlan chargePlan,
            final String debtorId,
            final BigDecimal amountOverride,
            final Integer rotationOrder,
            final ChargePlanMemberStatus status,
            final BigDecimal creditBalance,
            final Instant joinedAt,
            final Instant leftAt) {
        this(null, chargePlan, debtorId, amountOverride, rotationOrder, status, creditBalance, joinedAt, leftAt, null);
    }

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

    private static <T> T validateOrThrows(final T value, final String valueName) {
        return DomainValidation.validateOrThrows(value, valueName, DomainExceptionCode.INVALID_VALUE::createException);
    }
}
