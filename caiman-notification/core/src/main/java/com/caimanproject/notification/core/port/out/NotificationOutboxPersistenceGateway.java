package com.caimanproject.notification.core.port.out;

import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationOutboxPersistenceGateway {

    NotificationOutbox save(NotificationOutbox outbox);

    List<NotificationOutbox> findEligibleForDispatch(Instant now, int limit);

    List<NotificationOutbox> findStuckProcessing(Instant threshold);

    void delete(UUID id);
}
