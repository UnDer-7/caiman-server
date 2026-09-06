package com.caimanproject.notification.core.port.out;

import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import java.time.Instant;

public interface NotifyInvoiceSentGateway {

    void notify(NotificationOutbox outbox, Instant sentAt);
}
