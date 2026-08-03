package com.caimanproject.billing.core.domain.service;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.model.ChargePlanMember;
import com.caimanproject.billing.core.domain.model.ChargePlanNotificationConfig;
import com.caimanproject.billing.core.domain.types.BusinessExceptionCode;
import com.caimanproject.billing.core.port.in.CreateChargePlanUseCase;
import com.caimanproject.billing.core.port.in.command.CreateChargePlanCommand;
import com.caimanproject.billing.core.port.in.command.CreateChargePlanMemberCommand;
import com.caimanproject.billing.core.port.out.ChargePlanPersistenceGateway;
import com.caimanproject.contracts.exception.BusinessException;
import com.caimanproject.contracts.gateway.DebtorGateway;
import com.caimanproject.contracts.validation.ValidationError;
import com.caimanproject.contracts.validation.ValidationErrorSourceBody;
import com.caimanproject.contracts.validation.ValidationResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateChargePlanService implements CreateChargePlanUseCase {

    private final ChargePlanPersistenceGateway chargePlanPersistenceGateway;
    private final DebtorGateway debtorGateway;

    @Override
    public ChargePlan execute(final CreateChargePlanCommand chargePlanCommand) {
        final var chargePlan = buildChargePlan(chargePlanCommand);
        return chargePlanPersistenceGateway.save(chargePlan);
    }

    private ChargePlan buildChargePlan(final CreateChargePlanCommand command) {
        final List<ChargePlanMember> members = command.members().stream()
                .map(member -> ChargePlanMember.createBuilder()
                        .debtorId(member.debtorId())
                        .amountOverride(member.amountOverride())
                        .rotationOrder(member.rotationOrder())
                        .creditBalance(member.creditBalance())
                        .build())
                .toList();

        final List<ChargePlanNotificationConfig> notificationConfigs = command.notificationConfigs().stream()
                .map(notificationConfig -> ChargePlanNotificationConfig.createBuilder()
                        .triggerType(notificationConfig.triggerType())
                        .reminderInterval(notificationConfig.reminderInterval())
                        .reminderUnit(notificationConfig.reminderUnit())
                        .maxAttempts(notificationConfig.maxAttempts())
                        .build())
                .toList();

        final var validationDebtorsExists = validateDebtorExistence(command);
        final var validationDuplicateMembers = ChargePlan.validateDuplicateMembersByDebtorId(
                members, BusinessExceptionCode.DUPLICATE_CHARGE_PLAN_MEMBER_BY_DEBTOR_ID);
        final var validationEndsAt =
                ChargePlan.validateEndsAt(command.endsAt(), command.startsAt(), BusinessExceptionCode.INVALID_ENDS_AT);

        final ValidationResult validationType =
                switch (command.type()) {
                    case ROTATING -> validateTypeRotating(members);
                    case SPLIT -> validateTypeSplit(members, command.totalAmount());
                };

        validationDebtorsExists
                .merge(validationDuplicateMembers)
                .merge(validationEndsAt)
                .merge(validationType)
                .throwIfInvalid(BusinessException::new);

        return ChargePlan.createBuilder()
                .name(command.name())
                .description(command.description())
                .type(command.type())
                .proofValidationMode(command.proofValidationMode())
                .totalAmount(command.totalAmount())
                .dueToleranceDays(command.dueToleranceDays())
                .cycleUnit(command.cycleUnit())
                .cycleInterval(command.cycleInterval())
                .cycleAnchorDate(command.cycleAnchorDate())
                .notificationsEnabled(command.notificationsEnabled())
                .notificationTime(command.notificationTime())
                .notificationTimezone(command.notificationTimezone())
                .startsAt(command.startsAt())
                .endsAt(command.endsAt())
                .endWhenRecovered(command.endWhenRecovered())
                .notificationConfigs(notificationConfigs)
                .members(members)
                .build();
    }

    private static ValidationResult validateTypeSplit(
            final List<ChargePlanMember> members, final BigDecimal totalAmount) {
        return ChargePlan.validateRotationOrderPresence(members, BusinessExceptionCode.ROTATION_ORDER_NOT_ALLOWED)
                .merge(validateSplitOverrideSum(members, totalAmount));
    }

    private static ValidationResult validateSplitOverrideSum(
            final List<ChargePlanMember> members, final BigDecimal totalAmount) {
        final var overrideSum = members.stream()
                .map(ChargePlanMember::getAmountOverride)
                .flatMap(Optional::stream)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (overrideSum.compareTo(totalAmount) > 0) {
            return ValidationResult.of(ValidationError.builder()
                    .code(BusinessExceptionCode.SPLIT_OVERRIDE_SUM_EXCEEDS_TOTAL_AMOUNT)
                    .source(new ValidationErrorSourceBody("$.members[*].amountOverride", overrideSum.toString()))
                    .detail("sum of amountOverride (" + overrideSum + ") exceeds totalAmount (" + totalAmount + ")")
                    .build());
        }

        return ValidationResult.valid();
    }

    private static ValidationResult validateTypeRotating(final List<ChargePlanMember> members) {
        final var validationRotationOrderPresent =
                ChargePlan.validateMembersWithoutRotationOrder(members, BusinessExceptionCode.INVALID_ROTATION_ORDER);

        final ValidationResult validationRotationOrderGaps;
        if (validationRotationOrderPresent.isValid()) {
                validationRotationOrderGaps = ChargePlan.validateRotationOrderGaps(members, BusinessExceptionCode.ROTATION_ORDER_GAP);
        } else {
            validationRotationOrderGaps = ValidationResult.valid();
        }

        return validationRotationOrderPresent.merge(validationRotationOrderGaps);
    }

    private ValidationResult validateDebtorExistence(final CreateChargePlanCommand chargePlanCommand) {
        final var debtorIds = chargePlanCommand.members().stream()
                .map(CreateChargePlanMemberCommand::debtorId)
                .collect(Collectors.toSet());

        final var validations = debtorGateway.findMissingIds(debtorIds).stream()
                .map(id -> ValidationError.builder()
                        .code(BusinessExceptionCode.DEBTOR_NOT_FOUND)
                        .source(new ValidationErrorSourceBody("$.members[*].debtorId", id.toString()))
                        .build())
                .toList();
        return ValidationResult.of(validations);
    }
}
