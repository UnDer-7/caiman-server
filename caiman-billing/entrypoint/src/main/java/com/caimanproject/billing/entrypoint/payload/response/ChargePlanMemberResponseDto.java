package com.caimanproject.billing.entrypoint.payload.response;

import com.caimanproject.billing.core.domain.types.ChargePlanMemberStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record ChargePlanMemberResponseDto(
    @NotNull
    UUID id,

    @NotNull
    String debtorId,

    BigDecimal amountOverride,

    Integer rotationOrder,

    @NotNull
    ChargePlanMemberStatus status,

    @NotNull
    BigDecimal creditBalance,

    @NotNull
    Instant joinedAt,

    Instant leftAt,

    @Valid
    @NotNull
    AuditResponseDto audit
) {

}
