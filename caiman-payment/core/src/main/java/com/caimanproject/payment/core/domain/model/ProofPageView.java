package com.caimanproject.payment.core.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

@Builder
public record ProofPageView(
        String chargePlanName,
        String debtorName,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        Instant dueDate,
        int cycleIndex,
        boolean formEnabled,
        String statusMessage) {}
