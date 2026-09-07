package com.caimanproject.notification.core.test.builder;

import com.caimanproject.notification.core.domain.model.Audit;
import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import com.caimanproject.notification.core.domain.types.NotificationChannel;
import com.caimanproject.notification.core.domain.types.NotificationOutboxStatus;
import com.caimanproject.notification.core.domain.types.TriggerType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class NotificationOutboxDomainBuilder {

    private NotificationOutboxDomainBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static Audit.AuditBuilder buildAuditFull() {
        return Audit.builder().createdAt(Instant.now()).updatedAt(Instant.now());
    }

    public static NotificationOutbox.RestoreBuilder buildNotificationOutboxFull() {
        return NotificationOutbox.restoreBuilder()
                .id(UUID.randomUUID())
                .invoiceId(UUID.randomUUID())
                .triggerType(TriggerType.INVOICE_CREATED)
                .channel(NotificationChannel.EMAIL)
                .recipient("debtor@example.com")
                .debtorName("Jane Debtor")
                .planName("Shared YouTube Premium")
                .amountDue(new BigDecimal("90.00"))
                .dueDate(Instant.now())
                .uploadLink("https://example.com/upload")
                .cycleIndex(0L)
                .rejectionReason(null)
                .scheduledFor(Instant.now())
                .status(NotificationOutboxStatus.SCHEDULED)
                .attemptCount(0)
                .maxAttempts(5)
                .lastAttemptedAt(null)
                .lastError(null)
                .audit(buildAuditFull().build());
    }
}
