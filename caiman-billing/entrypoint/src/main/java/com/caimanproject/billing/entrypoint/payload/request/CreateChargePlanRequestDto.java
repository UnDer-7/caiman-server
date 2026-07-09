package com.caimanproject.billing.entrypoint.payload.request;

import com.caimanproject.billing.core.domain.types.ChargePlanStatus;
import com.caimanproject.billing.core.domain.types.ChargePlanType;
import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.ProofValidationMode;
import com.caimanproject.web.annotation.composition.body.MinBody;
import com.caimanproject.web.annotation.composition.body.NotBlankBody;
import com.caimanproject.web.annotation.composition.body.NotNullBody;
import com.caimanproject.web.annotation.composition.body.PositiveBody;
import com.caimanproject.web.annotation.composition.body.PositiveOrZeroBody;
import com.caimanproject.web.annotation.composition.body.SizeBody;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import lombok.Builder;

@Builder
public record CreateChargePlanRequestDto(
        @NotBlankBody @SizeBody(max = 255) String name,

        String description,

        @NotNullBody ChargePlanType type,

        @NotNullBody ChargePlanStatus status,

        @NotNullBody ProofValidationMode proofValidationMode,

        @NotNullBody @PositiveBody BigDecimal totalAmount,

        @NotNullBody @PositiveOrZeroBody Integer dueToleranceDays,

        @NotNullBody CycleUnit cycleUnit,

        @NotNullBody @MinBody(1) Integer cycleInterval,

        @NotNullBody LocalDate cycleAnchorDate,

        @NotNullBody Boolean notificationsEnabled,

        @NotNullBody LocalTime notificationTime,

        @NotNullBody ZoneId notificationTimezone,

        @NotNullBody Instant startsAt,

        Instant endsAt,

        @PositiveBody BigDecimal endWhenRecovered,

        @Valid List<CreateChargePlanNotificationConfigRequestDto> notificationConfigs,

        @Valid List<CreateChargePlanMemberRequestDto> members) {}
