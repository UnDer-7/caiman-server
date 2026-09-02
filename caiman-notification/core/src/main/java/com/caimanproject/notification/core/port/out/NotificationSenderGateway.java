package com.caimanproject.notification.core.port.out;

import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import com.caimanproject.notification.core.domain.types.NotificationChannel;

public interface NotificationSenderGateway {

    NotificationChannel supportedChannel();

    boolean send(NotificationOutbox outbox);
}
