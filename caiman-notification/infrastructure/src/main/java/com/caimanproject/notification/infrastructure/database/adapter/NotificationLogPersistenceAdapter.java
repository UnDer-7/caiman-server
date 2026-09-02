package com.caimanproject.notification.infrastructure.database.adapter;

import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import com.caimanproject.notification.core.domain.types.NotificationLogStatus;
import com.caimanproject.notification.core.port.out.NotificationLogPersistenceGateway;
import com.caimanproject.notification.infrastructure.database.entity.NotificationLogEntity;
import com.caimanproject.notification.infrastructure.database.repository.NotificationLogRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationLogPersistenceAdapter implements NotificationLogPersistenceGateway {

    private final NotificationLogRepository notificationLogRepository;

    @Override
    @Transactional
    public void logSent(final NotificationOutbox outbox, final Instant sentAt) {
        save(outbox, NotificationLogStatus.SENT, null, sentAt);
    }

    @Override
    @Transactional
    public void logFailed(final NotificationOutbox outbox, final String errorMessage, final Instant attemptedAt) {
        save(outbox, NotificationLogStatus.FAILED, errorMessage, attemptedAt);
    }

    private void save(
            final NotificationOutbox outbox,
            final NotificationLogStatus status,
            final String errorMessage,
            final Instant sentAt) {
        final NotificationLogEntity entity = NotificationLogEntity.builder()
                .invoiceId(outbox.getInvoiceId().toString())
                .outboxId(outbox.getId().orElseThrow().toString())
                .triggerType(outbox.getTriggerType())
                .channel(outbox.getChannel())
                .status(status)
                .recipient(outbox.getRecipient())
                .errorMessage(errorMessage)
                .sentAt(sentAt)
                .build();
        notificationLogRepository.save(entity);
    }
}
