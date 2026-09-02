package com.caimanproject.notification.infrastructure.database.adapter;

import com.caimanproject.notification.core.domain.types.NotificationLogStatus;
import com.caimanproject.notification.core.test.builder.NotificationOutboxDomainBuilder;
import com.caimanproject.notification.infrastructure.database.entity.NotificationLogEntity;
import com.caimanproject.notification.infrastructure.database.repository.NotificationLogRepository;
import com.caimanproject.test.annotation.UnitTest;
import java.time.Instant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class NotificationLogPersistenceAdapterTest {

    @Mock
    NotificationLogRepository notificationLogRepository;

    @InjectMocks
    NotificationLogPersistenceAdapter adapter;

    @Test
    void logSent_persists_a_sent_log_entry_built_from_the_outbox() {
        // Given
        final var outbox =
                NotificationOutboxDomainBuilder.buildNotificationOutboxFull().build();
        final var sentAt = Instant.now();
        Mockito.when(notificationLogRepository.save(Mockito.any(NotificationLogEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        adapter.logSent(outbox, sentAt);

        // Then
        final var captor = ArgumentCaptor.forClass(NotificationLogEntity.class);
        Mockito.verify(notificationLogRepository).save(captor.capture());
        final var saved = captor.getValue();
        Assertions.assertThat(saved.getInvoiceId())
                .isEqualTo(outbox.getInvoiceId().toString());
        Assertions.assertThat(saved.getOutboxId())
                .isEqualTo(outbox.getId().orElseThrow().toString());
        Assertions.assertThat(saved.getTriggerType()).isEqualTo(outbox.getTriggerType());
        Assertions.assertThat(saved.getChannel()).isEqualTo(outbox.getChannel());
        Assertions.assertThat(saved.getRecipient()).isEqualTo(outbox.getRecipient());
        Assertions.assertThat(saved.getStatus()).isEqualTo(NotificationLogStatus.SENT);
        Assertions.assertThat(saved.getErrorMessage()).isNull();
        Assertions.assertThat(saved.getSentAt()).isEqualTo(sentAt);
    }

    @Test
    void logFailed_persists_a_failed_log_entry_with_the_error_message() {
        // Given
        final var outbox =
                NotificationOutboxDomainBuilder.buildNotificationOutboxFull().build();
        final var attemptedAt = Instant.now();
        Mockito.when(notificationLogRepository.save(Mockito.any(NotificationLogEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        adapter.logFailed(outbox, "boom", attemptedAt);

        // Then
        final var captor = ArgumentCaptor.forClass(NotificationLogEntity.class);
        Mockito.verify(notificationLogRepository).save(captor.capture());
        final var saved = captor.getValue();
        Assertions.assertThat(saved.getStatus()).isEqualTo(NotificationLogStatus.FAILED);
        Assertions.assertThat(saved.getErrorMessage()).isEqualTo("boom");
        Assertions.assertThat(saved.getSentAt()).isEqualTo(attemptedAt);
    }
}
