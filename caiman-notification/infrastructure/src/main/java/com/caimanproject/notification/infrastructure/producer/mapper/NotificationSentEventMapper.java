package com.caimanproject.notification.infrastructure.producer.mapper;

import com.caimanproject.contracts.event.NotificationSentEventDto;
import com.caimanproject.contracts.util.Constants;
import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import java.time.Instant;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface NotificationSentEventMapper {

    @Mapping(target = "triggerType", expression = "java(outbox.getTriggerType().name())")
    NotificationSentEventDto toEventDto(NotificationOutbox outbox, Instant sentAt);
}
