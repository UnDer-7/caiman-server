package com.caimanproject.billing.entrypoint.payload.request;

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

@Builder
public record CreateChargePlanRequestDto(
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

    @NotNull
    ZoneId notificationTimezone,

    @NotNull
    Instant startsAt,

    @NotNull
    Instant endsAt,

    @NotNull
    BigDecimal endWhenRecovered,

    @Valid
    List<CreateChargePlanNotificationConfigRequestDto> notificationConfigs,

    @Valid
    List<CreateChargePlanMemberRequestDto> members
) {

}
