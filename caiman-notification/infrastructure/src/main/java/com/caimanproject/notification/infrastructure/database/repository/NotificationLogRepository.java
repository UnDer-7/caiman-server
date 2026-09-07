package com.caimanproject.notification.infrastructure.database.repository;

import com.caimanproject.notification.infrastructure.database.entity.NotificationLogEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationLogRepository extends CrudRepository<NotificationLogEntity, String> {}
