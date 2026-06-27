package com.caimanproject.billing.core.domain.model;

import com.caimanproject.billing.core.domain.exception.domain.DomainExceptionCode;
import com.caimanproject.billing.core.domain.types.ChargePlanStatus;
import com.caimanproject.billing.core.domain.types.ChargePlanType;
import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.ProofValidationMode;
import com.caimanproject.contracts.util.DomainValidation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ChargePlan {

    @Getter(AccessLevel.NONE)
    private final UUID id;

    private final String name;

    @Getter(AccessLevel.NONE)
    private final String description;

    private final ChargePlanType type;

    private final ChargePlanStatus status;

    private final ProofValidationMode proofValidationMode;

    private final BigDecimal totalAmount;

    private final Integer dueToleranceDays;

    private final CycleUnit cycleUnit;

    private final Integer cycleInterval;

    private final LocalDate cycleAnchorDate;

    private final Boolean notificationsEnabled;

    private final LocalTime notificationTime;

    private final String notificationTimezone;

    private final Instant startsAt;

    @Getter(AccessLevel.NONE)
    private final Instant endsAt;

    @Getter(AccessLevel.NONE)
    private final BigDecimal endWhenRecovered;

    private final Audit audit;

    private final List<ChargePlanNotificationConfig> notificationConfigs;

    private final List<ChargePlanMember> members;

    @Builder(builderMethodName = "restoreBuilder", builderClassName = "RestoreBuilder")
    public ChargePlan(
            final UUID id,
            final String name,
            final String description,
            final ChargePlanType type,
            final ChargePlanStatus status,
            final ProofValidationMode proofValidationMode,
            final BigDecimal totalAmount,
            final Integer dueToleranceDays,
            final CycleUnit cycleUnit,
            final Integer cycleInterval,
            final LocalDate cycleAnchorDate,
            final Boolean notificationsEnabled,
            final LocalTime notificationTime,
            final String notificationTimezone,
            final Instant startsAt,
            final Instant endsAt,
            final BigDecimal endWhenRecovered,
            final Audit audit,
            final List<ChargePlanNotificationConfig> notificationConfigs,
            final List<ChargePlanMember> members) {

        // Optional
        this.id = id;
        this.description = description;
        this.endsAt = endsAt;
        this.endWhenRecovered = endWhenRecovered;

        // Required
        this.name = validateOrThrows(name, "name");
        this.type = validateOrThrows(type, "type");
        this.status = validateOrThrows(status, "status");
        this.proofValidationMode = validateOrThrows(proofValidationMode, "proofValidationMode");
        this.totalAmount = validateOrThrows(totalAmount, "totalAmount");
        this.dueToleranceDays = validateOrThrows(dueToleranceDays, "dueToleranceDays");
        this.cycleUnit = validateOrThrows(cycleUnit, "cycleUnit");
        this.cycleInterval = validateOrThrows(cycleInterval, "cycleInterval");
        this.cycleAnchorDate = validateOrThrows(cycleAnchorDate, "cycleAnchorDate");
        this.notificationsEnabled = validateOrThrows(notificationsEnabled, "notificationsEnabled");
        this.notificationTime = validateOrThrows(notificationTime, "notificationTime");
        this.notificationTimezone = validateOrThrows(notificationTimezone, "notificationTimezone");
        this.startsAt = validateOrThrows(startsAt, "startsAt");
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);
        this.notificationConfigs = Optional.ofNullable(notificationConfigs)
                .filter(Predicate.not(List::isEmpty))
                .map(List::copyOf)
                .orElseGet(Collections::emptyList);
        this.members = Optional.ofNullable(members)
                .filter(Predicate.not(List::isEmpty))
                .map(List::copyOf)
                .orElseGet(Collections::emptyList);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public ChargePlan(
            final String name,
            final String description,
            final ChargePlanType type,
            final ChargePlanStatus status,
            final ProofValidationMode proofValidationMode,
            final BigDecimal totalAmount,
            final Integer dueToleranceDays,
            final CycleUnit cycleUnit,
            final Integer cycleInterval,
            final LocalDate cycleAnchorDate,
            final Boolean notificationsEnabled,
            final LocalTime notificationTime,
            final String notificationTimezone,
            final Instant startsAt,
            final Instant endsAt,
            final BigDecimal endWhenRecovered,
            final List<ChargePlanNotificationConfig> notificationConfigs,
            final List<ChargePlanMember> members) {
        this(
                null,
                name,
                description,
                type,
                status,
                proofValidationMode,
                totalAmount,
                dueToleranceDays,
                cycleUnit,
                cycleInterval,
                cycleAnchorDate,
                notificationsEnabled,
                notificationTime,
                notificationTimezone,
                startsAt,
                endsAt,
                endWhenRecovered,
                null,
                notificationConfigs,
                members);
    }

    public Optional<UUID> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description).filter(Predicate.not(String::isBlank));
    }

    public Optional<Instant> getEndsAt() {
        return Optional.ofNullable(endsAt);
    }

    public Optional<BigDecimal> getEndWhenRecovered() {
        return Optional.ofNullable(endWhenRecovered);
    }

    private static <T> T validateOrThrows(final T value, final String valueName) {
        return DomainValidation.validateOrThrows(value, valueName, DomainExceptionCode.INVALID_VALUE::createException);
    }
}
