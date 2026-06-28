package com.caimanproject.billing.core.domain.model;

import com.caimanproject.billing.core.domain.exception.domain.DomainExceptionCode;
import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.TriggerType;
import com.caimanproject.contracts.util.DomainValidation;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ChargePlanNotificationConfig {

    @Getter(AccessLevel.NONE)
    private final UUID id;

    private final TriggerType triggerType;

    @Getter(AccessLevel.NONE)
    private final Integer reminderInterval;

    @Getter(AccessLevel.NONE)
    private final CycleUnit reminderUnit;

    @Getter(AccessLevel.NONE)
    private final Integer maxAttempts;

    private final Boolean enabled;

    private final Audit audit;

    @Builder(builderMethodName = "restoreBuilder", builderClassName = "RestoreBuilder")
    public ChargePlanNotificationConfig(
            final UUID id,
            final TriggerType triggerType,
            final Integer reminderInterval,
            final CycleUnit reminderUnit,
            final Integer maxAttempts,
            final Boolean enabled,
            final Audit audit) {

        // Optional
        this.id = id;
        this.reminderInterval = reminderInterval;
        this.reminderUnit = reminderUnit;
        this.maxAttempts = maxAttempts;

        // Required
        this.triggerType = validateOrThrows(triggerType, "triggerType");
        this.enabled = validateOrThrows(enabled, "enabled");
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public ChargePlanNotificationConfig(
            final TriggerType triggerType,
            final Integer reminderInterval,
            final CycleUnit reminderUnit,
            final Integer maxAttempts,
            final Boolean enabled) {
        this(null, triggerType, reminderInterval, reminderUnit, maxAttempts, enabled, null);
    }

    public Optional<UUID> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<Integer> getReminderInterval() {
        return Optional.ofNullable(reminderInterval);
    }

    public Optional<CycleUnit> getReminderUnit() {
        return Optional.ofNullable(reminderUnit);
    }

    public Optional<Integer> getMaxAttempts() {
        return Optional.ofNullable(maxAttempts);
    }

    private static <T> T validateOrThrows(final T value, final String valueName) {
        return DomainValidation.validateOrThrows(value, valueName, DomainExceptionCode.INVALID_VALUE::createException);
    }
}
