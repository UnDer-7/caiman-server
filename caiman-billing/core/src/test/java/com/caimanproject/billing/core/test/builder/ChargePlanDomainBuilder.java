package com.caimanproject.billing.core.test.builder;

import com.caimanproject.billing.core.domain.model.Audit;
import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.model.ChargePlanMember;
import com.caimanproject.billing.core.domain.types.ChargePlanMemberStatus;
import com.caimanproject.billing.core.domain.types.ChargePlanStatus;
import com.caimanproject.billing.core.domain.types.ChargePlanType;
import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.ProofValidationMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public final class ChargePlanDomainBuilder {

    private ChargePlanDomainBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static Audit.AuditBuilder buildAuditFull() {
        return Audit.builder().createdAt(Instant.now()).updatedAt(Instant.now());
    }

    public static ChargePlanMember.RestoreBuilder buildChargePlanMemberFull() {
        return ChargePlanMember.restoreBuilder()
                .id(UUID.randomUUID())
                .debtorId(UUID.randomUUID())
                .amountOverride(null)
                .rotationOrder(1)
                .status(ChargePlanMemberStatus.ACTIVE)
                .creditBalance(BigDecimal.ZERO)
                .joinedAt(Instant.now())
                .leftAt(null)
                .audit(buildAuditFull().build());
    }

    public static ChargePlanMember.RestoreBuilder buildSplitChargePlanMemberFull() {
        return buildChargePlanMemberFull().rotationOrder(null);
    }

    public static ChargePlan.RestoreBuilder buildSplitChargePlanDueTodayFull() {
        return ChargePlan.restoreBuilder()
                .id(UUID.randomUUID())
                .name("AirBnB Trip")
                .description("Split plan")
                .type(ChargePlanType.SPLIT)
                .status(ChargePlanStatus.ACTIVE)
                .proofValidationMode(ProofValidationMode.MANUAL)
                .totalAmount(new BigDecimal("100.00"))
                .dueToleranceDays(5)
                .cycleUnit(CycleUnit.DAILY)
                .cycleInterval(1)
                .cycleAnchorDate(LocalDate.now())
                .notificationsEnabled(true)
                .notificationTime(LocalTime.NOON)
                .notificationTimezone(ZoneId.of("UTC"))
                .startsAt(Instant.now())
                .audit(buildAuditFull().build())
                .members(List.of(buildSplitChargePlanMemberFull().build()));
    }

    public static ChargePlan.RestoreBuilder buildRotatingChargePlanDueTodayFull() {
        return ChargePlan.restoreBuilder()
                .id(UUID.randomUUID())
                .name("Shared YouTube Premium")
                .description("Rotating plan")
                .type(ChargePlanType.ROTATING)
                .status(ChargePlanStatus.ACTIVE)
                .proofValidationMode(ProofValidationMode.MANUAL)
                .totalAmount(new BigDecimal("90.00"))
                .dueToleranceDays(5)
                .cycleUnit(CycleUnit.DAILY)
                .cycleInterval(1)
                .cycleAnchorDate(LocalDate.now())
                .notificationsEnabled(true)
                .notificationTime(LocalTime.NOON)
                .notificationTimezone(ZoneId.of("UTC"))
                .startsAt(Instant.now())
                .audit(buildAuditFull().build())
                .members(List.of(buildChargePlanMemberFull().build()));
    }
}
