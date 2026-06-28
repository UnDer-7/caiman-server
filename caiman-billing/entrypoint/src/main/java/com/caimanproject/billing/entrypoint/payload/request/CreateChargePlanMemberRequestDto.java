package com.caimanproject.billing.entrypoint.payload.request;

import com.caimanproject.billing.core.domain.types.ChargePlanMemberStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record CreateChargePlanMemberRequestDto(
    @NotNull
    String debtorId,

    BigDecimal amountOverride,

    Integer rotationOrder,

    @NotNull
    ChargePlanMemberStatus status,

    @NotNull
    BigDecimal creditBalance,

    @NotNull
    Instant joinedAt
) {

}
