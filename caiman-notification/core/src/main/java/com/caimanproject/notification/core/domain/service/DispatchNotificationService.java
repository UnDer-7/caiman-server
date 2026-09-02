package com.caimanproject.notification.core.domain.service;

import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import com.caimanproject.notification.core.domain.types.NotificationChannel;
import com.caimanproject.notification.core.port.out.NotificationLogPersistenceGateway;
import com.caimanproject.notification.core.port.out.NotificationOutboxPersistenceGateway;
import com.caimanproject.notification.core.port.out.NotificationSenderGateway;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DispatchNotificationService {

    private final Map<NotificationChannel, NotificationSenderGateway> sendersByChannel;
    private final NotificationOutboxPersistenceGateway outboxGateway;
    private final NotificationLogPersistenceGateway logGateway;

    public DispatchNotificationService(
            final List<NotificationSenderGateway> senders,
            final NotificationOutboxPersistenceGateway outboxGateway,
            final NotificationLogPersistenceGateway logGateway) {
        this.sendersByChannel = senders.stream()
                .collect(Collectors.toMap(NotificationSenderGateway::supportedChannel, Function.identity()));
        this.outboxGateway = outboxGateway;
        this.logGateway = logGateway;
    }

    public void dispatch(final NotificationOutbox outbox, final Instant now) {
        final var processing = outboxGateway.save(outbox.markProcessing(now));

        final NotificationSenderGateway sender = sendersByChannel.get(processing.getChannel());
        if (sender == null) {
            log.warn("no dispatcher implemented for channel {}, skipping", processing.getChannel());
            return;
        }

        final boolean success = sender.send(processing);

        if (success) {
            logGateway.logSent(processing, now);
            outboxGateway.delete(processing.getId().orElseThrow());
            // TODO: notify invoice sent status
            return;
        }

        final String errorMessage = "Notification dispatch failed";
        final Instant nextRetry = now.plus(nextBackoff(processing.getAttemptCount()));
        final var attempted = processing.markFailedAndReschedule(nextRetry, now, errorMessage);

        if (attempted.isExhausted()) {
            logGateway.logFailed(attempted, errorMessage, now);
            outboxGateway.delete(attempted.getId().orElseThrow());
        } else {
            outboxGateway.save(attempted);
            logGateway.logFailed(attempted, errorMessage, now);
        }
    }

    private static Duration nextBackoff(final int attemptCountBeforeThisFailure) {
        final long minutes = Math.min(1L << (attemptCountBeforeThisFailure + 1), 60L);
        return Duration.ofMinutes(minutes);
    }
}
