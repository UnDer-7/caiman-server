package com.caimanproject.contracts.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record InvoiceEventDto(
        UUID id,

        UUID chargePlanId,

        String chargePlanName,

        UUID chargePlanMemberId,

        UUID debtorId,

        Boolean invoiceCreatedNotificationEnabled,

        Long cycleIndex,

        LocalDate generationDate,

        BigDecimal amountDue,

        Instant dueDate,

        String uploadLink,

        Instant scheduledFor,

        Integer maxAttempts,

        AuditEventDto audit) {

    public static final String EVENT_TYPE_GENERATED = "invoice_generated";
}
