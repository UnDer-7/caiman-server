package com.caimanproject.contracts.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record InvoiceEventDto(
    UUID id,

    UUID chargePlanId,

    String chargePlanName,

    UUID chargePlanMemberId,

    Long cycleIndex,

    LocalDate generationDate,

    BigDecimal amountDue,

    Instant dueDate,

    Instant scheduledFor,

    Integer maxAttempts,

    AuditEventDto audit
) {

    public static final String EVENT_TYPE_GENERATED = "invoice_generated";

}
