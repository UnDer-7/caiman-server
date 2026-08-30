package com.caimanproject.notification.infrastructure.database.mapper;

import com.caimanproject.contracts.util.Constants;
import com.caimanproject.jpa.AuditEmbeddable;
import com.caimanproject.mapper.OptionalMapper;
import com.caimanproject.notification.core.domain.model.Audit;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {OptionalMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface NotificationAuditEntityMapper {

    AuditEmbeddable toEntity(Audit model);

    Audit toDto(AuditEmbeddable model);
}
