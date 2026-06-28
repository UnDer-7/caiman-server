package com.caimanproject.billing.entrypoint.payload.response;

import com.caimanproject.billing.core.domain.model.Audit;
import com.caimanproject.billing.core.domain.types.ChargePlanStatus;
import com.caimanproject.billing.core.domain.types.ChargePlanType;
import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.ProofValidationMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    UUID id,

    @NotBlank
    String name,

    String description,

    @NotNull
    ChargePlanType type,

    @NotNull
    ChargePlanStatus status,

    @NotNull
    ProofValidationMode proofValidationMode,

    @NotNull
    BigDecimal totalAmount,

    @NotNull
    Integer dueToleranceDays,

    @NotNull
    CycleUnit cycleUnit,

    @NotNull
    Integer cycleInterval,

    @NotNull
    LocalDate cycleAnchorDate,

    @NotNull
    Boolean notificationsEnabled,

    @NotNull
    LocalTime notificationTime,

    @NotBlank
    ZoneId notificationTimezone,

    @NotNull
    Instant startsAt,

    @NotNull
    Instant endsAt,

    @NotNull
    BigDecimal endWhenRecovered,

    @Valid
    @NotNull
    List<ChargePlanNotificationConfigResponseDto> notificationConfigs,

    @Valid
    @NotNull
    List<ChargePlanMemberResponseDto> members,

    @NotNull
    @Valid
    AuditResponseDto audit
) {

}
