package com.caimanproject.billing.core.port.in.command;

import com.caimanproject.billing.core.domain.types.ChargePlanMemberStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record CreateChargePlanMemberCommand(
    UUID debtorId,

    BigDecimal amountOverride,

    Integer rotationOrder,

    BigDecimal creditBalance,

    Instant joinedAt
) {

}
