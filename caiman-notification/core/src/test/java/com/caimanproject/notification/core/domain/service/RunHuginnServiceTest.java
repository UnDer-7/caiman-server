package com.caimanproject.notification.core.domain.service;

import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import com.caimanproject.notification.core.domain.types.NotificationOutboxStatus;
import com.caimanproject.notification.core.port.out.NotificationLogPersistenceGateway;
import com.caimanproject.notification.core.port.out.NotificationOutboxPersistenceGateway;
import com.caimanproject.notification.core.test.builder.NotificationOutboxDomainBuilder;
import com.caimanproject.test.annotation.UnitTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RunHuginnServiceTest {

    @Mock
    NotificationOutboxPersistenceGateway outboxGateway;

    @Mock
    NotificationLogPersistenceGateway logGateway;

    @Mock
    DispatchNotificationService dispatchNotificationService;

    @InjectMocks
    RunHuginnService service;

    @Test
    void should_stop_looping_once_a_batch_comes_back_empty() {
        // Given
        Mockito.when(outboxGateway.findStuckProcessing(ArgumentMatchers.any())).thenReturn(List.of());
        final var first =
                NotificationOutboxDomainBuilder.buildNotificationOutboxFull().build();
        Mockito.when(outboxGateway.findEligibleForDispatch(ArgumentMatchers.any(), ArgumentMatchers.anyInt()))
                .thenReturn(List.of(first))
                .thenReturn(List.of());

        // When
        service.execute();

        // Then
        Mockito.verify(outboxGateway, Mockito.times(2))
                .findEligibleForDispatch(ArgumentMatchers.any(), ArgumentMatchers.anyInt());
        Mockito.verify(dispatchNotificationService).dispatch(ArgumentMatchers.eq(first), ArgumentMatchers.any());
    }

    @Test
    void should_reset_stuck_processing_rows_back_to_scheduled_when_not_exhausted() {
        // Given
        final var stuck = NotificationOutboxDomainBuilder.buildNotificationOutboxFull()
                .status(NotificationOutboxStatus.PROCESSING)
                .attemptCount(1)
                .maxAttempts(5)
                .build();
        Mockito.when(outboxGateway.findStuckProcessing(ArgumentMatchers.any())).thenReturn(List.of(stuck));
        Mockito.when(outboxGateway.findEligibleForDispatch(ArgumentMatchers.any(), ArgumentMatchers.anyInt()))
                .thenReturn(List.of());

        // When
        service.execute();

        // Then
        Mockito.verify(outboxGateway)
                .save(ArgumentMatchers.argThat(o -> o.getStatus() == NotificationOutboxStatus.SCHEDULED));
        Mockito.verify(outboxGateway, Mockito.never()).delete(ArgumentMatchers.any());
        Mockito.verifyNoInteractions(logGateway);
    }

    @Test
    void should_delete_and_log_stuck_processing_rows_that_are_exhausted_after_reset() {
        // Given
        final var stuck = NotificationOutboxDomainBuilder.buildNotificationOutboxFull()
                .status(NotificationOutboxStatus.PROCESSING)
                .attemptCount(5)
                .maxAttempts(5)
                .build();
        Mockito.when(outboxGateway.findStuckProcessing(ArgumentMatchers.any())).thenReturn(List.of(stuck));
        Mockito.when(outboxGateway.findEligibleForDispatch(ArgumentMatchers.any(), ArgumentMatchers.anyInt()))
                .thenReturn(List.of());

        // When
        service.execute();

        // Then
        Mockito.verify(outboxGateway).delete(stuck.getId().orElseThrow());
        Mockito.verify(logGateway)
                .logFailed(
                        ArgumentMatchers.any(NotificationOutbox.class),
                        ArgumentMatchers.eq("Max attempts reached while stuck in PROCESSING state"),
                        ArgumentMatchers.any(Instant.class));
        Mockito.verify(outboxGateway, Mockito.never()).save(ArgumentMatchers.any());
    }
}
