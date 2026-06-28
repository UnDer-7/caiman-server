package com.caimanproject.billing.core.port.in.command;

import com.caimanproject.billing.core.domain.types.ChargePlanStatus;
import com.caimanproject.billing.core.domain.types.ChargePlanType;
import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.ProofValidationMode;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Builder
public record CreateChargePlanCommand(
    String name,

    String description,

    ChargePlanType type,

    ChargePlanStatus status,

    ProofValidationMode proofValidationMode,

    BigDecimal totalAmount,

    Integer dueToleranceDays,

    CycleUnit cycleUnit,

    Integer cycleInterval,

    LocalDate cycleAnchorDate,

    Boolean notificationsEnabled,

    LocalTime notificationTime,

    ZoneId notificationTimezone,

    Instant startsAt,

    Instant endsAt,

    BigDecimal endWhenRecovered,

    List<CreateChargePlanNotificationConfigCommand> notificationConfigs,

    List<CreateChargePlanMemberCommand> members
) {

}
