package com.caimanproject.billing.entrypoint.payload.response;

import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.TriggerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ChargePlanNotificationConfigResponseDto(
    @NotNull
    UUID id,

    @NotNull
    TriggerType triggerType,

    Integer reminderInterval,

    CycleUnit reminderUnit,

    Integer maxAttempts,

    @NotNull
    Boolean enabled,

    @Valid
    @NotNull
    AuditResponseDto audit
) {

}
