package com.caimanproject.notification.infrastructure.email.adapter;

import com.caimanproject.contracts.util.LogMask;
import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import com.caimanproject.notification.core.domain.types.NotificationChannel;
import com.caimanproject.notification.core.port.out.NotificationSenderGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationSenderAdapter implements NotificationSenderGateway {

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean send(final NotificationOutbox outbox) {
        log.info(
                "Sending email MOCK — recipient={}, triggerType={}",
                LogMask.email(outbox.getRecipient()),
                outbox.getTriggerType());
        return true;
    }
}
