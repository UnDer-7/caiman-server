package com.caimanproject.notification.core.domain.model;

import com.caimanproject.notification.core.domain.types.NotificationOutboxStatus;
import com.caimanproject.notification.core.test.builder.NotificationOutboxDomainBuilder;
import com.caimanproject.test.annotation.UnitTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@UnitTest
class NotificationOutboxTest {

    @Test
    void markProcessing_sets_status_processing_and_last_attempted_at() {
        // Given
        final var outbox = NotificationOutboxDomainBuilder.buildNotificationOutboxFull()
                .status(NotificationOutboxStatus.SCHEDULED)
                .lastAttemptedAt(null)
                .build();
        final var now = Instant.now();

        // When
        final var result = outbox.markProcessing(now);

        // Then
        Assertions.assertThat(result.getStatus()).isEqualTo(NotificationOutboxStatus.PROCESSING);
        Assertions.assertThat(result.getLastAttemptedAt()).contains(now);
        Assertions.assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.SCHEDULED);
    }

    @Test
    void markFailedAndReschedule_increments_attempt_count_and_sets_retry_scheduled() {
        // Given
        final var outbox = NotificationOutboxDomainBuilder.buildNotificationOutboxFull()
                .attemptCount(1)
                .build();
        final var attemptedAt = Instant.now();
        final var nextRetry = attemptedAt.plus(4, ChronoUnit.MINUTES);

        // When
        final var result = outbox.markFailedAndReschedule(nextRetry, attemptedAt, "boom");

        // Then
        Assertions.assertThat(result.getStatus()).isEqualTo(NotificationOutboxStatus.RETRY_SCHEDULED);
        Assertions.assertThat(result.getAttemptCount()).isEqualTo(2);
        Assertions.assertThat(result.getScheduledFor()).isEqualTo(nextRetry);
        Assertions.assertThat(result.getLastAttemptedAt()).contains(attemptedAt);
        Assertions.assertThat(result.getLastError()).contains("boom");
        Assertions.assertThat(outbox.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void markStuckReset_resets_to_scheduled_and_increments_attempt_count() {
        // Given
        final var outbox = NotificationOutboxDomainBuilder.buildNotificationOutboxFull()
                .status(NotificationOutboxStatus.PROCESSING)
                .attemptCount(2)
                .build();

        // When
        final var result = outbox.markStuckReset();

        // Then
        Assertions.assertThat(result.getStatus()).isEqualTo(NotificationOutboxStatus.SCHEDULED);
        Assertions.assertThat(result.getAttemptCount()).isEqualTo(3);
        Assertions.assertThat(result.getLastError()).contains("Reset from stuck PROCESSING state");
    }

    @Test
    void isExhausted_true_when_attempt_count_reaches_max_attempts() {
        // Given
        final var outbox = NotificationOutboxDomainBuilder.buildNotificationOutboxFull()
                .attemptCount(5)
                .maxAttempts(5)
                .build();

        // Then
        Assertions.assertThat(outbox.isExhausted()).isTrue();
    }

    @Test
    void isExhausted_false_when_attempt_count_below_max_attempts() {
        // Given
        final var outbox = NotificationOutboxDomainBuilder.buildNotificationOutboxFull()
                .attemptCount(4)
                .maxAttempts(5)
                .build();

        // Then
        Assertions.assertThat(outbox.isExhausted()).isFalse();
    }

    @Test
    void isRetryScheduled_true_when_status_retry_scheduled() {
        // Given
        final var outbox = NotificationOutboxDomainBuilder.buildNotificationOutboxFull()
                .status(NotificationOutboxStatus.RETRY_SCHEDULED)
                .build();

        // Then
        Assertions.assertThat(outbox.isRetryScheduled()).isTrue();
    }
}
