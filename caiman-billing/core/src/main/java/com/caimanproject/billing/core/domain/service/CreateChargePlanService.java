package com.caimanproject.billing.core.domain.service;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.model.ChargePlanMember;
import com.caimanproject.billing.core.domain.model.ChargePlanNotificationConfig;
import com.caimanproject.billing.core.port.in.CreateChargePlanUseCase;
import com.caimanproject.billing.core.port.in.command.CreateChargePlanCommand;
import com.caimanproject.billing.core.port.in.command.CreateChargePlanMemberCommand;
import com.caimanproject.billing.core.port.out.ChargePlanPersistenceGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateChargePlanService implements CreateChargePlanUseCase {

    private final ChargePlanPersistenceGateway chargePlanPersistenceGateway;

    @Override
    public ChargePlan execute(final CreateChargePlanCommand chargePlanCommand) {
        final var chargePlan = buildChargePlan(chargePlanCommand);
        return chargePlanPersistenceGateway.save(chargePlan);
    }

    private ChargePlan buildChargePlan(final CreateChargePlanCommand command) {
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
            .notificationConfigs(command.notificationConfigs().stream().map(
                        notificationConfig -> ChargePlanNotificationConfig.createBuilder()
                            .triggerType(notificationConfig.triggerType())
                            .reminderInterval(notificationConfig.reminderInterval())
                            .reminderUnit(notificationConfig.reminderUnit())
                            .maxAttempts(notificationConfig.maxAttempts())
                            .enabled(notificationConfig.enabled())
                            .build())
                    .toList())
            .members(command.members().stream().map(
                member -> ChargePlanMember.createBuilder()
                .debtorId(member.debtorId())
                .amountOverride(member.amountOverride())
                .rotationOrder(member.rotationOrder())
                .status(member.status())
                .creditBalance(member.creditBalance())
                .joinedAt(member.joinedAt())
                .build()).toList())
            .build();
    }

}
