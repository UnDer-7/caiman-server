package com.caimanproject.notification.core.port.in.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateInvoiceCreatedOutboxCommand(
        UUID invoiceId,
        UUID debtorId,
        Boolean invoiceCreatedNotificationEnabled,
        String planName,
        Long cycleIndex,
        BigDecimal amountDue,
        Instant dueDate,
        String uploadLink,
        Instant scheduledFor,
        Integer maxAttempts) {}
