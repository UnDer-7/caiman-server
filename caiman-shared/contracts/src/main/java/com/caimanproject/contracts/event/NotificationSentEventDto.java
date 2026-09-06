package com.caimanproject.contracts.event;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record NotificationSentEventDto(UUID invoiceId, String triggerType, Instant sentAt) {

    public static final String EVENT_TYPE_NOTIFICATION_SENT = "notification_sent";

    public static final String TRIGGER_TYPE_INVOICE_CREATED = "INVOICE_CREATED";
}
