package com.caimanproject.billing.entrypoint.payload.request;

import com.caimanproject.web.annotation.composition.body.NotNullBody;
import com.caimanproject.web.annotation.composition.body.PositiveBody;
import com.caimanproject.web.annotation.composition.body.PositiveOrZeroBody;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record CreateChargePlanMemberRequestDto(
    @NotNullBody
    UUID debtorId,

    @PositiveBody
    BigDecimal amountOverride,

    @PositiveBody
    Integer rotationOrder,

    @NotNullBody
    @PositiveOrZeroBody
    BigDecimal creditBalance,

    @NotNullBody
    Instant joinedAt
) {

}
