package com.caimanproject.notification.core.port.out;

import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import java.time.Instant;

public interface NotificationLogPersistenceGateway {

    void logSent(NotificationOutbox outbox, Instant sentAt);

    void logFailed(NotificationOutbox outbox, String errorMessage, Instant attemptedAt);
}
