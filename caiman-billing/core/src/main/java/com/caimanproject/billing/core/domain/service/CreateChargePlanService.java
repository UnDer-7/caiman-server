package com.caimanproject.billing.core.domain.service;

import com.caimanproject.billing.core.domain.exception.business.BusinessExceptionCode;
import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.model.ChargePlanMember;
import com.caimanproject.billing.core.domain.model.ChargePlanNotificationConfig;
import com.caimanproject.billing.core.domain.types.ChargePlanType;
import com.caimanproject.billing.core.port.in.CreateChargePlanUseCase;
import com.caimanproject.billing.core.port.in.command.CreateChargePlanCommand;
import com.caimanproject.billing.core.port.in.command.CreateChargePlanMemberCommand;
import com.caimanproject.billing.core.port.out.ChargePlanPersistenceGateway;
import com.caimanproject.contracts.gateway.DebtorGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
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

        validateDebtorExistence(command);
        validateDuplicateMembers(members);

        if (command.type() == ChargePlanType.SPLIT) {
            validateTypeSplit();
        } else {
            validateTypeRotating(command, members);
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

    }

    private void validateTypeRotating(final CreateChargePlanCommand chargePlanCommand, final List<ChargePlanMember> members) {
        final var membersWithoutRotationOrder = ChargePlan.getMembersWithoutRotationOrder(members);
        if (!membersWithoutRotationOrder.isEmpty()) {
            final var errorMsg = membersWithoutRotationOrder.stream()
                .map(e -> e.getDebtorId().toString())
                .collect(Collectors.joining(","));
            throw BusinessExceptionCode.INVALID_ROTATION_ORDER.createException("ROTATING plan requires rotationOrder for all members. Missing (debtorId): [%s]".formatted(errorMsg));
        }
        if (ChargePlan.hasRotationOrderGaps(members)) {
            final var errorMsg = members.stream()
                .filter(member -> member.getRotationOrder().isPresent())
                .sorted(Comparator.comparing(member -> member.getRotationOrder().get()))
                .map(member -> "debtorId: %s - rotationOrder: %s".formatted(member.getDebtorId(), member.getRotationOrder().get()))
                .collect(Collectors.joining(","));

            throw BusinessExceptionCode.INVALID_ROTATION_ORDER.createException("rotationOrder values must form a sequence starting at 1 with no gaps (order of members does not matter). Members informed: [%s]".formatted(errorMsg));
        }
    }

    private void validateDebtorExistence(final CreateChargePlanCommand chargePlanCommand) {
        final var debtorIds = chargePlanCommand.members().stream()
            .map(CreateChargePlanMemberCommand::debtorId)
            .collect(Collectors.toSet());
        final var notFoundDebtorIds = debtorGateway.findMissingIds(debtorIds);
        if (!notFoundDebtorIds.isEmpty()) {
            final var notFoundDebtorIdsMsg = notFoundDebtorIds.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(", "));

            throw BusinessExceptionCode.DEBTOR_NOT_FOUND.createException("The given debtor IDs were not found: [%s]".formatted(notFoundDebtorIdsMsg));
        }
    }

    private void validateDuplicateMembers(final List<ChargePlanMember> members) {
        final var duplicateMembers = ChargePlan.getDuplicateMembersByDebtorId(members);
        if (!duplicateMembers.isEmpty()) {
            final var msg = duplicateMembers.stream()
                .map(cpm -> "debtorId: %s"
                    .formatted(cpm.getDebtorId()))
                .collect(Collectors.joining(", "));
            throw BusinessExceptionCode.DUPLICATE_CHARGE_PLAN_MEMBER_BY_DEBTOR_ID.createException(msg);
        }
    }

}
