package com.caimanproject.notification.core.domain.service;

import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import com.caimanproject.notification.core.port.in.RunHuginnUseCase;
import com.caimanproject.notification.core.port.out.NotificationLogPersistenceGateway;
import com.caimanproject.notification.core.port.out.NotificationOutboxPersistenceGateway;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunHuginnService implements RunHuginnUseCase {

    private static final int BATCH_SIZE = 50; // todo: pegar via envs
    private static final Duration STUCK_PROCESSING_THRESHOLD = Duration.ofMinutes(5); // todo: pegar via envs

    private final NotificationOutboxPersistenceGateway notificationOutboxPersistenceGateway;
    private final NotificationLogPersistenceGateway notificationLogPersistenceGateway;
    private final DispatchNotificationService dispatchNotificationService;

    @Override
    public void execute() {
        final Instant now = Instant.now(); // captured once for the whole run — never re-read mid-run

        resetStuckProcessing(now);

        List<NotificationOutbox> batch;
        do {
            batch = notificationOutboxPersistenceGateway.findEligibleForDispatch(now, BATCH_SIZE);
            batch.forEach(entry -> dispatchNotificationService.dispatch(entry, now));
        } while (!batch.isEmpty());
    }

    private void resetStuckProcessing(final Instant now) {
        final Instant threshold = now.minus(STUCK_PROCESSING_THRESHOLD);
        notificationOutboxPersistenceGateway.findStuckProcessing(threshold).forEach(entry -> {
            final var reset = entry.markStuckReset();
            if (reset.isExhausted()) {
                notificationLogPersistenceGateway.logFailed(reset, "Max attempts reached while stuck in PROCESSING state", now);
                notificationOutboxPersistenceGateway.delete(reset.getId().orElseThrow());
            } else {
                notificationOutboxPersistenceGateway.save(reset);
            }
        });
    }
}
