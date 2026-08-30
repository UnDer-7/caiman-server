package com.caimanproject.notification.infrastructure.database.adapter;

import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import com.caimanproject.notification.core.port.out.NotificationOutboxPersistenceGateway;
import com.caimanproject.notification.infrastructure.database.entity.NotificationOutboxEntity;
import com.caimanproject.notification.infrastructure.database.mapper.NotificationOutboxEntityMapper;
import com.caimanproject.notification.infrastructure.database.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxPersistenceAdapter implements NotificationOutboxPersistenceGateway {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationOutboxEntityMapper notificationOutboxEntityMapper;

    @Override
    @Transactional
    public NotificationOutbox save(final NotificationOutbox outbox) {
        final NotificationOutboxEntity entity = notificationOutboxEntityMapper.toEntity(outbox);
        final NotificationOutboxEntity saved = notificationOutboxRepository.save(entity);
        return notificationOutboxEntityMapper.toModel(saved);
    }
}
