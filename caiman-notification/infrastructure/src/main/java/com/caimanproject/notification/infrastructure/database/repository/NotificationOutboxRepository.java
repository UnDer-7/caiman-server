package com.caimanproject.notification.infrastructure.database.repository;

import com.caimanproject.notification.infrastructure.database.entity.NotificationOutboxEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationOutboxRepository extends CrudRepository<NotificationOutboxEntity, String> {}
