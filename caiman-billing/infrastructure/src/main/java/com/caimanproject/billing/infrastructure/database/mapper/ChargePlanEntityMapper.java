package com.caimanproject.billing.infrastructure.database.mapper;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.infrastructure.database.entity.ChargePlanEntity;
import com.caimanproject.contracts.util.Constants;
import com.caimanproject.mapper.IdMapper;
import com.caimanproject.mapper.OptionalMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {
            OptionalMapper.class,
            IdMapper.class,
            BillingAuditEntityMapper.class,
            ChargePlanMemberEntityMapper.class,
            ChargePlanNotificationConfigEntityMapper.class
        },
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ChargePlanEntityMapper {

    ChargePlan toModel(ChargePlanEntity entity);

    @Mapping(target = "members", ignore = true)
    @Mapping(target = "notificationConfigs", ignore = true)
    ChargePlanEntity toEntity(ChargePlan model);
}
