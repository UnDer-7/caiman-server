package com.caimanproject.notification.infrastructure.database.mapper;

import com.caimanproject.contracts.util.Constants;
import com.caimanproject.mapper.IdMapper;
import com.caimanproject.mapper.OptionalMapper;
import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import com.caimanproject.notification.infrastructure.database.entity.NotificationOutboxEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {OptionalMapper.class, IdMapper.class, NotificationAuditEntityMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface NotificationOutboxEntityMapper {

    NotificationOutbox toModel(NotificationOutboxEntity entity);

    NotificationOutboxEntity toEntity(NotificationOutbox model);
}
