package com.caimanproject.notification.core.domain.service;

import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import com.caimanproject.notification.core.domain.types.NotificationChannel;
import com.caimanproject.notification.core.domain.types.NotificationOutboxStatus;
import com.caimanproject.notification.core.port.out.NotificationLogPersistenceGateway;
import com.caimanproject.notification.core.port.out.NotificationOutboxPersistenceGateway;
import com.caimanproject.notification.core.port.out.NotificationSenderGateway;
import com.caimanproject.notification.core.port.out.NotifyInvoiceSentGateway;
import com.caimanproject.notification.core.test.builder.NotificationOutboxDomainBuilder;
import com.caimanproject.test.annotation.UnitTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DispatchNotificationServiceTest {

    @Mock
    NotificationOutboxPersistenceGateway outboxGateway;

    @Mock
    NotificationLogPersistenceGateway logGateway;

    @Mock
    NotificationSenderGateway emailSenderGateway;

    @Mock
    NotifyInvoiceSentGateway notifyInvoiceSentGateway;

    DispatchNotificationService service;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(emailSenderGateway.supportedChannel()).thenReturn(NotificationChannel.EMAIL);
        service = new DispatchNotificationService(
                List.of(emailSenderGateway), outboxGateway, logGateway, notifyInvoiceSentGateway);
    }

    @Test
    void should_delete_outbox_and_log_sent_when_send_succeeds() {
        // Given
        final var outbox =
                NotificationOutboxDomainBuilder.buildNotificationOutboxFull().build();
        final var now = Instant.now();
        Mockito.when(outboxGateway.save(Mockito.any(NotificationOutbox.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(emailSenderGateway.send(Mockito.any(NotificationOutbox.class)))
                .thenReturn(true);

        // When
        service.dispatch(outbox, now);

        // Then
        final var processingCaptor = ArgumentCaptor.forClass(NotificationOutbox.class);
        Mockito.verify(outboxGateway).save(processingCaptor.capture());
        Assertions.assertThat(processingCaptor.getValue().getStatus()).isEqualTo(NotificationOutboxStatus.PROCESSING);

        Mockito.verify(logGateway).logSent(Mockito.any(NotificationOutbox.class), Mockito.eq(now));
        Mockito.verify(outboxGateway).delete(outbox.getId().orElseThrow());
        Mockito.verify(logGateway, Mockito.never()).logFailed(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(notifyInvoiceSentGateway).notify(Mockito.any(NotificationOutbox.class), Mockito.eq(now));
    }

    @Test
    void should_reschedule_outbox_and_log_failed_when_send_fails_and_not_exhausted() {
        // Given
        final var outbox = NotificationOutboxDomainBuilder.buildNotificationOutboxFull()
                .attemptCount(0)
                .maxAttempts(5)
                .build();
        final var now = Instant.now();
        Mockito.when(outboxGateway.save(Mockito.any(NotificationOutbox.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(emailSenderGateway.send(Mockito.any(NotificationOutbox.class)))
                .thenReturn(false);

        // When
        service.dispatch(outbox, now);

        // Then
        final var savedCaptor = ArgumentCaptor.forClass(NotificationOutbox.class);
        Mockito.verify(outboxGateway, Mockito.times(2)).save(savedCaptor.capture());
        final var rescheduled = savedCaptor.getAllValues().get(1);
        Assertions.assertThat(rescheduled.getStatus()).isEqualTo(NotificationOutboxStatus.RETRY_SCHEDULED);
        Assertions.assertThat(rescheduled.getAttemptCount()).isEqualTo(1);

        Mockito.verify(logGateway)
                .logFailed(Mockito.any(NotificationOutbox.class), Mockito.anyString(), Mockito.eq(now));
        Mockito.verify(outboxGateway, Mockito.never()).delete(Mockito.any(UUID.class));
    }

    @Test
    void should_delete_outbox_and_log_failed_when_send_fails_and_exhausted() {
        // Given
        final var outbox = NotificationOutboxDomainBuilder.buildNotificationOutboxFull()
                .attemptCount(4)
                .maxAttempts(5)
                .build();
        final var now = Instant.now();
        Mockito.when(outboxGateway.save(Mockito.any(NotificationOutbox.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(emailSenderGateway.send(Mockito.any(NotificationOutbox.class)))
                .thenReturn(false);

        // When
        service.dispatch(outbox, now);

        // Then
        Mockito.verify(outboxGateway).delete(outbox.getId().orElseThrow());
        Mockito.verify(logGateway)
                .logFailed(Mockito.any(NotificationOutbox.class), Mockito.anyString(), Mockito.eq(now));
        Mockito.verify(outboxGateway, Mockito.times(1)).save(Mockito.any(NotificationOutbox.class));
    }

    @Test
    void should_skip_and_leave_processing_when_no_sender_for_channel() {
        // Given
        final var noSenderService =
                new DispatchNotificationService(List.of(), outboxGateway, logGateway, notifyInvoiceSentGateway);
        final var outbox =
                NotificationOutboxDomainBuilder.buildNotificationOutboxFull().build();
        final var now = Instant.now();
        Mockito.when(outboxGateway.save(Mockito.any(NotificationOutbox.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        noSenderService.dispatch(outbox, now);

        // Then
        Mockito.verify(outboxGateway, Mockito.never()).delete(Mockito.any(UUID.class));
        Mockito.verifyNoInteractions(logGateway);
        Mockito.verifyNoInteractions(notifyInvoiceSentGateway);
    }
}
