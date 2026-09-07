package com.caimanproject.billing.entrypoint.payload.response;

import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.TriggerType;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ChargePlanNotificationConfigResponseDto(
        UUID id,

        TriggerType triggerType,

        Integer reminderInterval,

        CycleUnit reminderUnit,

        Integer maxAttempts,

        Boolean enabled,

        AuditResponseDto audit) {}
