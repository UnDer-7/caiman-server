package com.caimanproject.billing.core.domain.model;

import com.caimanproject.billing.core.domain.types.ChargePlanStatus;
import com.caimanproject.billing.core.domain.types.ChargePlanType;
import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.DomainExceptionCode;
import com.caimanproject.billing.core.domain.types.ProofValidationMode;
import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.contracts.exception.ExceptionCode;
import com.caimanproject.contracts.util.DomainValidation;
import com.caimanproject.contracts.validation.ValidationError;
import com.caimanproject.contracts.validation.ValidationErrorSourceBody;
import com.caimanproject.contracts.validation.ValidationResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

    private final ZoneId notificationTimezone;

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
            final ZoneId notificationTimezone,
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
        this.name = name;
        this.type = type;
        this.status = status;
        this.proofValidationMode = proofValidationMode;
        this.totalAmount = totalAmount;
        this.dueToleranceDays = dueToleranceDays;
        this.cycleUnit = cycleUnit;
        this.cycleInterval = cycleInterval;
        this.cycleAnchorDate = cycleAnchorDate;
        this.notificationsEnabled = notificationsEnabled;
        this.notificationTime = notificationTime;
        this.notificationTimezone = notificationTimezone;
        this.startsAt = startsAt;
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);
        this.notificationConfigs = Optional.ofNullable(notificationConfigs)
                .filter(Predicate.not(List::isEmpty))
                .map(List::copyOf)
                .orElseGet(Collections::emptyList);
        this.members = Optional.ofNullable(members)
                .filter(Predicate.not(List::isEmpty))
                .map(List::copyOf)
                .orElseGet(Collections::emptyList);

        final var fieldValidations = DomainValidation.validateAll(List.of(
                DomainValidation.validate(name, "$.name", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(type, "$.type", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(status, "$.status", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(
                        proofValidationMode, "$.proofValidationMode", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(totalAmount, "$.totalAmount", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(dueToleranceDays, "$.dueToleranceDays", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(cycleUnit, "$.cycleUnit", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(cycleInterval, "$.cycleInterval", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(cycleAnchorDate, "$.cycleAnchorDate", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(
                        notificationsEnabled, "$.notificationsEnabled", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(notificationTime, "$.notificationTime", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(
                        notificationTimezone, "$.notificationTimezone", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(startsAt, "$.startsAt", DomainExceptionCode.INVALID_VALUE)));

        fieldValidations.throwIfInvalid(DomainException::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public ChargePlan(
            final String name,
            final String description,
            final ChargePlanType type,
            final ProofValidationMode proofValidationMode,
            final BigDecimal totalAmount,
            final Integer dueToleranceDays,
            final CycleUnit cycleUnit,
            final Integer cycleInterval,
            final LocalDate cycleAnchorDate,
            final Boolean notificationsEnabled,
            final LocalTime notificationTime,
            final ZoneId notificationTimezone,
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
                ChargePlanStatus.ACTIVE,
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

    public Optional<ChargePlanNotificationConfig> getInvoiceCreatedNotification() {
        return notificationConfigs.stream()
            .filter(ChargePlanNotificationConfig::isTriggerTypeInvoiceCreated)
            .findFirst();
    }

    public List<ChargePlanMember> getActiveMembers() {
        return getMembers().stream()
            .filter(ChargePlanMember::isActive)
            .toList();
    }

    public List<ChargePlanMember> getActiveMembersOrderedByRotation() {
        final var activeMembers = getActiveMembers();

        final var missingRotationOrder = activeMembers.stream()
                .filter(member -> member.getRotationOrder().isEmpty())
                .map(member -> ValidationError.builder()
                        .code(DomainExceptionCode.INVALID_VALUE)
                        .source(new ValidationErrorSourceBody("$.members[*].rotationOrder", null))
                        .detail("debtorId: " + member.getDebtorId())
                        .build())
                .toList();
        ValidationResult.of(missingRotationOrder).throwIfInvalid(DomainException::new);

        return activeMembers.stream()
                .sorted(Comparator.comparingInt(member -> member.getRotationOrder().orElseThrow()))
                .toList();
    }

    public ChargePlan withUpdatedMember(final ChargePlanMember updatedMember) {
        final var updatedMembers = members.stream()
                .map(member -> member.getId().equals(updatedMember.getId()) ? updatedMember : member)
                .toList();

        return ChargePlan.restoreBuilder()
                .id(id)
                .name(name)
                .description(description)
                .type(type)
                .status(status)
                .proofValidationMode(proofValidationMode)
                .totalAmount(totalAmount)
                .dueToleranceDays(dueToleranceDays)
                .cycleUnit(cycleUnit)
                .cycleInterval(cycleInterval)
                .cycleAnchorDate(cycleAnchorDate)
                .notificationsEnabled(notificationsEnabled)
                .notificationTime(notificationTime)
                .notificationTimezone(notificationTimezone)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .endWhenRecovered(endWhenRecovered)
                .audit(audit)
                .notificationConfigs(notificationConfigs)
                .members(updatedMembers)
                .build();
    }

    public Instant dueDateFrom(final LocalDate generationDate) {
        return generationDate.plusDays(dueToleranceDays).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public UUID requireId() {
        return getId().orElseThrow(() -> new DomainException(List.of(ValidationError.builder()
                .code(DomainExceptionCode.INVALID_VALUE)
                .source(new ValidationErrorSourceBody("$.chargePlan.id", null))
                .detail("ChargePlan returned by search gateway without a persisted id")
                .build())));
    }

    public boolean isGenerationDueOn(final LocalDate currentDate) {
        if (currentDate.isBefore(startsAt.atZone(ZoneOffset.UTC).toLocalDate())) {
            return false;
        }

        if (currentDate.isBefore(cycleAnchorDate)) {
            return false;
        }
        
        return switch (cycleUnit) {
            case DAILY -> ChronoUnit.DAYS.between(cycleAnchorDate, currentDate) % cycleInterval == 0;
            case WEEKLY -> ChronoUnit.DAYS.between(cycleAnchorDate, currentDate) % (cycleInterval * 7L) == 0;
            case MONTHLY -> isMonthlyDue(currentDate);
        };
    }

    public static ValidationResult validateDuplicateMembersByDebtorId(
            final List<ChargePlanMember> members, final ExceptionCode exceptionCode) {
        final var validations =
                members.stream().collect(Collectors.groupingBy(ChargePlanMember::getDebtorId)).values().stream()
                        .filter(group -> group.size() > 1)
                        .map(List::getFirst)
                        .map(member -> ValidationError.builder()
                                .code(exceptionCode)
                                .source(new ValidationErrorSourceBody(
                                        "$.members[*].debtorId",
                                        member.getDebtorId().toString()))
                                .build())
                        .toList();
        return ValidationResult.of(validations);
    }

    public static ValidationResult validateRotationOrderPresence(
            final List<ChargePlanMember> members, final ExceptionCode exceptionCode) {
        final var validations = members.stream()
                .filter(cp -> cp.getRotationOrder().isPresent())
                .map(member -> ValidationError.builder()
                        .code(exceptionCode)
                        .source(new ValidationErrorSourceBody(
                                "$.members[*].rotationOrder",
                                member.getRotationOrder().get().toString()))
                        .detail("debtorId: " + member.getDebtorId().toString())
                        .build())
                .toList();
        return ValidationResult.of(validations);
    }

    public static ValidationResult validateMembersWithoutRotationOrder(
            final List<ChargePlanMember> members, final ExceptionCode exceptionCode) {
        final var validations = members.stream()
                .filter(member -> member.getRotationOrder().isEmpty())
                .map(member -> ValidationError.builder()
                        .code(exceptionCode)
                        .source(new ValidationErrorSourceBody("$.members[*].rotationOrder", null))
                        .detail("debtorId: " + member.getDebtorId().toString())
                        .build())
                .toList();
        return ValidationResult.of(validations);
    }

    public static ValidationResult validateEndsAt(
            final Instant endsAt, final Instant startsAt, final ExceptionCode exceptionCode) {
        if (endsAt == null) {
            return ValidationResult.valid();
        }

        if (!endsAt.isAfter(startsAt)) {
            return ValidationResult.of(ValidationError.builder()
                    .code(exceptionCode)
                    .source(new ValidationErrorSourceBody("$.endsAt", endsAt.toString()))
                    .build());
        }

        return ValidationResult.valid();
    }

    public static ValidationResult validateRotationOrderGaps(
            final List<ChargePlanMember> members, final ExceptionCode exceptionCode) {
        if (members.isEmpty()) {
            return ValidationResult.valid();
        }

        final List<Integer> rotationOrders = members.stream()
                .map(ChargePlanMember::getRotationOrder)
                .flatMap(Optional::stream)
                .sorted()
                .toList();
        final var hasGap = rotationOrders.getFirst() != 1
                || IntStream.range(1, rotationOrders.size())
                        .anyMatch(i -> rotationOrders.get(i) - rotationOrders.get(i - 1) != 1);

        if (!hasGap) {
            return ValidationResult.valid();
        }

        final var invalidValues = rotationOrders.stream().map(String::valueOf).collect(Collectors.joining(", "));
        final var detail = members.stream()
                .filter(member -> member.getRotationOrder().isPresent())
                .map(m -> "debtorId: %s - rotationOrder: %s"
                        .formatted(m.getDebtorId(), m.getRotationOrder().get()))
                .collect(Collectors.joining(" | "));
        final var validationError = ValidationError.builder()
                .code(exceptionCode)
                .detail(detail)
                .source(new ValidationErrorSourceBody("$.members[*].rotationOrder", invalidValues))
                .build();
        return ValidationResult.of(validationError);
    }

    private boolean isMonthlyDue(final LocalDate currentDate) {
        final long monthsBetween = ChronoUnit.MONTHS.between(cycleAnchorDate.withDayOfMonth(1), currentDate.withDayOfMonth(1));

        if (monthsBetween % cycleInterval != 0) {
            return false;
        }

        final int targetDay = Math.min(cycleAnchorDate.getDayOfMonth(), currentDate.lengthOfMonth());
        return currentDate.getDayOfMonth() == targetDay;
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
}
