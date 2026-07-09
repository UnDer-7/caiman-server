package com.caimanproject.billing.entrypoint.payload.response;

import com.caimanproject.billing.core.domain.types.ChargePlanMemberStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record ChargePlanMemberResponseDto(
    UUID id,

    String debtorId,

    BigDecimal amountOverride,

    Integer rotationOrder,

    ChargePlanMemberStatus status,

    BigDecimal creditBalance,

    Instant joinedAt,

    Instant leftAt,

    AuditResponseDto audit
) {

}
