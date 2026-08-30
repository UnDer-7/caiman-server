package com.caimanproject.notification.core.port.out;

import com.caimanproject.notification.core.domain.model.NotificationOutbox;

public interface NotificationOutboxPersistenceGateway {

    NotificationOutbox save(NotificationOutbox outbox);
}
