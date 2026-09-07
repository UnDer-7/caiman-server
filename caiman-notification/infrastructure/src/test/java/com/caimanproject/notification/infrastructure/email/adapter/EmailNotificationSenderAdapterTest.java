package com.caimanproject.notification.infrastructure.email.adapter;

import com.caimanproject.notification.core.domain.types.NotificationChannel;
import com.caimanproject.notification.core.test.builder.NotificationOutboxDomainBuilder;
import com.caimanproject.test.annotation.UnitTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@UnitTest
class EmailNotificationSenderAdapterTest {

    private final EmailNotificationSenderAdapter adapter = new EmailNotificationSenderAdapter();

    @Test
    void supportedChannel_is_email() {
        Assertions.assertThat(adapter.supportedChannel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Test
    void send_always_returns_true_mock() {
        final var outbox =
                NotificationOutboxDomainBuilder.buildNotificationOutboxFull().build();

        Assertions.assertThat(adapter.send(outbox)).isTrue();
    }
}
