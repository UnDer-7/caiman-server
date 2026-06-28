package com.caimanproject.billing.core.domain.types;

public enum TriggerType {
    INVOICE_CREATED,
    PENDING_REMINDER,
    OVERDUE_REMINDER,
    PAYMENT_APPROVED,
    PAYMENT_REJECTED
}
