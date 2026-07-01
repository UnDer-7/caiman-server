package com.caimanproject.billing.entrypoint.payload.request;

import com.caimanproject.billing.core.domain.types.ChargePlanStatus;
import com.caimanproject.billing.core.domain.types.ChargePlanType;
import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.ProofValidationMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
    @Positive
    BigDecimal totalAmount,

    @NotNull
    @PositiveOrZero
    Integer dueToleranceDays,

    @NotNull
    CycleUnit cycleUnit,

    @NotNull
    @Min(1)
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

    Instant endsAt,

    @Positive
    BigDecimal endWhenRecovered,

    @Valid
    List<CreateChargePlanNotificationConfigRequestDto> notificationConfigs,

    @Valid
    List<CreateChargePlanMemberRequestDto> members
) {

}
