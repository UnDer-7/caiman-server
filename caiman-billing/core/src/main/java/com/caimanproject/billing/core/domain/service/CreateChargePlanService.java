package com.caimanproject.billing.core.domain.service;

import com.caimanproject.billing.core.domain.types.BusinessExceptionCode;
import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.model.ChargePlanMember;
import com.caimanproject.billing.core.domain.model.ChargePlanNotificationConfig;
import com.caimanproject.billing.core.domain.types.ChargePlanType;
import com.caimanproject.billing.core.port.in.CreateChargePlanUseCase;
import com.caimanproject.billing.core.port.in.command.CreateChargePlanCommand;
import com.caimanproject.billing.core.port.in.command.CreateChargePlanMemberCommand;
import com.caimanproject.billing.core.port.out.ChargePlanPersistenceGateway;
import com.caimanproject.contracts.exception.BusinessException;
import com.caimanproject.contracts.gateway.DebtorGateway;
import com.caimanproject.contracts.validation.ValidationError;
import com.caimanproject.contracts.validation.ValidationErrorSourceBody;
import com.caimanproject.contracts.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
        final List<ChargePlanMember> members = command.members().stream().map(
            member -> ChargePlanMember.createBuilder()
                .debtorId(member.debtorId())
                .amountOverride(member.amountOverride())
                .rotationOrder(member.rotationOrder())
                .creditBalance(member.creditBalance())
                .joinedAt(member.joinedAt())
                .build()).toList();

        final List<ChargePlanNotificationConfig> notificationConfigs = command.notificationConfigs().stream().map(
                notificationConfig -> ChargePlanNotificationConfig.createBuilder()
                    .triggerType(notificationConfig.triggerType())
                    .reminderInterval(notificationConfig.reminderInterval())
                    .reminderUnit(notificationConfig.reminderUnit())
                    .maxAttempts(notificationConfig.maxAttempts())
                    .enabled(notificationConfig.enabled())
                    .build())
            .toList();

        final var validationDebtorsExists = validateDebtorExistence(command);
        final var validationDuplicateMembers = validateDuplicateMembers(members);

        if (command.type() == ChargePlanType.SPLIT) {
            validateTypeSplit();
        } else {
            final var validationTypeRotation = validateTypeRotating(command, members);
            validationDebtorsExists.merge(validationDuplicateMembers).merge(validationTypeRotation).throwIfInvalid(BusinessException::new);
        }

        return ChargePlan.createBuilder()
            .name(command.name())
            .description(command.description())
            .type(command.type())
            .status(command.status())
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

    private void validateTypeSplit() {
        // todo: fazer depois
    }

    private static ValidationResult validateTypeRotating(final CreateChargePlanCommand chargePlanCommand, final List<ChargePlanMember> members) {
        // todo: terminar de validar o rotation
        final var validationRotationOrder = ChargePlan.validateMembersWithoutRotationOrder(members, BusinessExceptionCode.INVALID_ROTATION_ORDER);
        final var validationRotationOrderGaps = ChargePlan.validateRotationOrderGaps(members, BusinessExceptionCode.INVALID_ROTATION_ORDER);
        return validationRotationOrder.merge(validationRotationOrderGaps);
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

    private static ValidationResult validateDuplicateMembers(final List<ChargePlanMember> members) {
        return ChargePlan.validateDuplicateMembersByDebtorId(members, BusinessExceptionCode.DUPLICATE_CHARGE_PLAN_MEMBER_BY_DEBTOR_ID);
    }
}
