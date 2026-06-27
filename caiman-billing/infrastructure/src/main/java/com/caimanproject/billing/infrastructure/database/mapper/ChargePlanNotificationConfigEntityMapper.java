package com.caimanproject.billing.infrastructure.database.mapper;

import com.caimanproject.billing.core.domain.model.ChargePlanNotificationConfig;
import com.caimanproject.billing.infrastructure.database.entity.ChargePlanNotificationConfigEntity;
import com.caimanproject.contracts.util.Constants;
import com.caimanproject.mapper.IdMapper;
import com.caimanproject.mapper.OptionalMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Collection;

@Mapper(
    componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    uses = {OptionalMapper.class, IdMapper.class, BillingAuditEntityMapper.class},
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ChargePlanNotificationConfigEntityMapper {

    ChargePlanNotificationConfig toModel(ChargePlanNotificationConfigEntity entity);

    @Mapping(target = "chargePlan", ignore = true)
    ChargePlanNotificationConfigEntity toEntity(ChargePlanNotificationConfig model);

    Collection<ChargePlanNotificationConfigEntity> toEntity(Collection<ChargePlanNotificationConfig> model);
}
