package com.caimanproject.billing.core.domain.model;

import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.DomainExceptionCode;
import com.caimanproject.billing.core.domain.types.TriggerType;
import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.contracts.util.DomainValidation;
import java.util.List;
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
        this.triggerType = triggerType;
        this.enabled = enabled;
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);

        final var fieldValidations = DomainValidation.validateAll(List.of(
                DomainValidation.validate(triggerType, "$.triggerType", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(enabled, "$.enabled", DomainExceptionCode.INVALID_VALUE)));

        fieldValidations.throwIfInvalid(DomainException::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public ChargePlanNotificationConfig(
            final TriggerType triggerType,
            final Integer reminderInterval,
            final CycleUnit reminderUnit,
            final Integer maxAttempts) {
        this(null, triggerType, reminderInterval, reminderUnit, maxAttempts, true, null);
    }

    public boolean isPendingReminder() {
        return getTriggerType() == TriggerType.PENDING_REMINDER;
    }

    public boolean isOverdueReminder() {
        return getTriggerType() == TriggerType.OVERDUE_REMINDER;
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
}
