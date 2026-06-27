package com.caimanproject.billing.infrastructure.database.mapper;

import com.caimanproject.billing.core.domain.model.ChargePlanMember;
import com.caimanproject.billing.infrastructure.database.entity.ChargePlanMemberEntity;
import com.caimanproject.contracts.util.Constants;
import com.caimanproject.mapper.IdMapper;
import com.caimanproject.mapper.OptionalMapper;
import java.util.Collection;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {OptionalMapper.class, IdMapper.class, BillingAuditEntityMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ChargePlanMemberEntityMapper {

    ChargePlanMember toModel(ChargePlanMemberEntity entity);

    @Mapping(target = "chargePlan", ignore = true)
    ChargePlanMemberEntity toEntity(ChargePlanMember model);

    Collection<ChargePlanMemberEntity> toEntity(Collection<ChargePlanMember> models);
}
