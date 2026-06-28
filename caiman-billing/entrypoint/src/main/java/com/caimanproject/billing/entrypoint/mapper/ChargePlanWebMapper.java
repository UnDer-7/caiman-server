package com.caimanproject.billing.entrypoint.mapper;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.port.in.command.CreateChargePlanCommand;
import com.caimanproject.billing.entrypoint.payload.request.CreateChargePlanRequestDto;
import com.caimanproject.billing.entrypoint.payload.response.ChargePlanResponseDto;
import com.caimanproject.contracts.util.Constants;
import com.caimanproject.mapper.OptionalMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    uses = {OptionalMapper.class, BillingAuditWebMapper.class, CreateChargePlanNotificationConfigWebMapper.class, CreateChargePlanMemberWebMapper.class},
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ChargePlanWebMapper {

    CreateChargePlanCommand toCommand(CreateChargePlanRequestDto dto);

    ChargePlanResponseDto toDto(ChargePlan chargePlan);
}
