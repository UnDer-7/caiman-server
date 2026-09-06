package com.caimanproject.payment.entrypoint.payload.response;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

@Builder
public record ProofPageResponseDto(
        String chargePlanName,
        String debtorName,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        Instant dueDate,
        int cycleIndex,
        boolean formEnabled,
        String statusMessage) {}
