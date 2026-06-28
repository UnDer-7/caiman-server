package com.caimanproject.billing.core.port.in.command;

import com.caimanproject.billing.core.domain.types.ChargePlanMemberStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record CreateChargePlanMemberCommand(
    String debtorId,

    BigDecimal amountOverride,

    Integer rotationOrder,

    ChargePlanMemberStatus status,

    BigDecimal creditBalance,

    Instant joinedAt
) {

}
