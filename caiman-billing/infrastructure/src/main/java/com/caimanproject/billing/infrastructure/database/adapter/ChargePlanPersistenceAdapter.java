package com.caimanproject.billing.infrastructure.database.adapter;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.port.out.ChargePlanPersistenceGateway;
import com.caimanproject.billing.infrastructure.database.mapper.ChargePlanEntityMapper;
import com.caimanproject.billing.infrastructure.database.mapper.ChargePlanMemberEntityMapper;
import com.caimanproject.billing.infrastructure.database.mapper.ChargePlanNotificationConfigEntityMapper;
import com.caimanproject.billing.infrastructure.database.repository.ChargePlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChargePlanPersistenceAdapter implements ChargePlanPersistenceGateway {

    private final ChargePlanRepository chargePlanRepository;
    private final ChargePlanEntityMapper chargePlanEntityMapper;
    private final ChargePlanMemberEntityMapper chargePlanMemberEntityMapper;
    private final ChargePlanNotificationConfigEntityMapper chargePlanNotificationConfigEntityMapper;

    @Override
    public ChargePlan save(final ChargePlan chargePlan) {
        final var chargePlanEntity = chargePlanEntityMapper.toEntity(chargePlan);
        final var chargePlanMembersEntity = chargePlanMemberEntityMapper.toEntity(chargePlan.getMembers());
        final var chargePlanNotificationsEntity = chargePlanNotificationConfigEntityMapper.toEntity(chargePlan.getNotificationConfigs());
        chargePlanEntity.addMembers(chargePlanMembersEntity);
        chargePlanEntity.addNotificationConfigs(chargePlanNotificationsEntity);

        final var chargePlanSaved = chargePlanRepository.save(chargePlanEntity);

        return chargePlanEntityMapper.toModel(chargePlanSaved);
    }
}
