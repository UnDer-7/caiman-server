package com.caimanproject.billing.core.port.in.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateChargePlanMemberCommand(
        UUID debtorId, BigDecimal amountOverride, Integer rotationOrder, BigDecimal creditBalance, Instant joinedAt) {}
