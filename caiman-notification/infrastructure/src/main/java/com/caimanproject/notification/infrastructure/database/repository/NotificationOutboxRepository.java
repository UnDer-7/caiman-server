package com.caimanproject.notification.infrastructure.database.repository;

import com.caimanproject.notification.infrastructure.database.entity.NotificationOutboxEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationOutboxRepository extends CrudRepository<NotificationOutboxEntity, String> {

    @Query("""
        SELECT n FROM NotificationOutboxEntity n
        WHERE n.status IN (com.caimanproject.notification.core.domain.types.NotificationOutboxStatus.SCHEDULED,
                            com.caimanproject.notification.core.domain.types.NotificationOutboxStatus.RETRY_SCHEDULED)
          AND n.scheduledFor <= :now
        ORDER BY n.scheduledFor ASC
        """)
    List<NotificationOutboxEntity> findEligibleForDispatch(@Param("now") Instant now, Pageable pageable);

    @Query("""
        SELECT n FROM NotificationOutboxEntity n
        WHERE n.status = com.caimanproject.notification.core.domain.types.NotificationOutboxStatus.PROCESSING
          AND n.lastAttemptedAt < :threshold
        """)
    List<NotificationOutboxEntity> findStuckProcessing(@Param("threshold") Instant threshold);
}
