package com.caimanproject.billing.entrypoint.payload.response;

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
import java.util.UUID;

@Builder
public record ChargePlanResponseDto(
    UUID id,

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

    List<ChargePlanNotificationConfigResponseDto> notificationConfigs,

    List<ChargePlanMemberResponseDto> members,

    AuditResponseDto audit
) {

}
