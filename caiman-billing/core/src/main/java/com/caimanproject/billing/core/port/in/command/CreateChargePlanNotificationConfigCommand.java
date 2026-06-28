package com.caimanproject.billing.core.port.in.command;

import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.TriggerType;
import lombok.Builder;

@Builder
public record CreateChargePlanNotificationConfigCommand(
    TriggerType triggerType,

    Integer reminderInterval,

    CycleUnit reminderUnit,

    Integer maxAttempts,

    Boolean enabled
) {

}
