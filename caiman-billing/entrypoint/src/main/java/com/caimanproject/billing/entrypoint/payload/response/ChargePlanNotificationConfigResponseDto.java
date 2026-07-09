package com.caimanproject.billing.entrypoint.payload.response;

import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.TriggerType;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ChargePlanNotificationConfigResponseDto(
    UUID id,

    TriggerType triggerType,

    Integer reminderInterval,

    CycleUnit reminderUnit,

    Integer maxAttempts,

    Boolean enabled,

    AuditResponseDto audit
) {

}
