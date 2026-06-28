package com.caimanproject.billing.entrypoint.mapper;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.model.ChargePlanMember;
import com.caimanproject.billing.core.port.in.command.CreateChargePlanMemberCommand;
import com.caimanproject.billing.entrypoint.payload.request.CreateChargePlanMemberRequestDto;
import com.caimanproject.billing.entrypoint.payload.response.ChargePlanMemberResponseDto;
import com.caimanproject.contracts.util.Constants;
import com.caimanproject.mapper.OptionalMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.Collection;

@Mapper(
    componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    uses = {OptionalMapper.class, BillingAuditWebMapper.class},
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CreateChargePlanMemberWebMapper {

    CreateChargePlanMemberCommand toCommand(CreateChargePlanMemberRequestDto dto);
    Collection<CreateChargePlanMemberCommand> toCommand(Collection<CreateChargePlanMemberRequestDto> dtos);

    ChargePlanMemberResponseDto toDto(ChargePlanMember chargePlan);
    Collection<ChargePlanMemberResponseDto> toDto(Collection<ChargePlanMember> chargePlans);
}
