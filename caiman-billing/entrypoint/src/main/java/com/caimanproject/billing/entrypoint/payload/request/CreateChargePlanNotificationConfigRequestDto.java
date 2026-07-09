package com.caimanproject.billing.entrypoint.payload.request;

import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.TriggerType;
import com.caimanproject.web.annotation.composition.body.NotNullBody;
import lombok.Builder;

@Builder
public record CreateChargePlanNotificationConfigRequestDto(
        @NotNullBody TriggerType triggerType,

        Integer reminderInterval,

        CycleUnit reminderUnit,

        Integer maxAttempts,

        @NotNullBody Boolean enabled) {}
