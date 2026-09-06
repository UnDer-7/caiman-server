package com.caimanproject.contracts.gateway.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record InvoiceSnapshotDto(
        UUID id,
        String chargePlanName,
        String debtorName,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        String status,
        Instant dueDate,
        int cycleIndex) {}
