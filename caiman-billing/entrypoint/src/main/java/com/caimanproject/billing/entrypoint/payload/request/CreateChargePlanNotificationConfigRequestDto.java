package com.caimanproject.billing.entrypoint.payload.request;

import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.TriggerType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateChargePlanNotificationConfigRequestDto(
    @NotNull
    TriggerType triggerType,

    Integer reminderInterval,

    CycleUnit reminderUnit,

    Integer maxAttempts,

    @NotNull
    Boolean enabled
) {

}
