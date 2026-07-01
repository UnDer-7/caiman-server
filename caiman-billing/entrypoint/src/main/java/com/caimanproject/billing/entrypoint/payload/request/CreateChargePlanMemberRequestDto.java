package com.caimanproject.billing.entrypoint.payload.request;

import com.caimanproject.billing.core.domain.types.ChargePlanMemberStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record CreateChargePlanMemberRequestDto(
    @NotNull
    UUID debtorId,

    @Positive
    BigDecimal amountOverride,

    @Positive
    Integer rotationOrder,

    @NotNull
    @PositiveOrZero
    BigDecimal creditBalance,

    @NotNull
    Instant joinedAt
) {

}
