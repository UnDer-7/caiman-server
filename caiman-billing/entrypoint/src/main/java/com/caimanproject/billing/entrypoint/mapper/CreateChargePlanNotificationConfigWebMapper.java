package com.caimanproject.billing.entrypoint.mapper;

import com.caimanproject.billing.core.domain.model.ChargePlanNotificationConfig;
import com.caimanproject.billing.core.port.in.command.CreateChargePlanNotificationConfigCommand;
import com.caimanproject.billing.entrypoint.payload.request.CreateChargePlanNotificationConfigRequestDto;
import com.caimanproject.billing.entrypoint.payload.response.ChargePlanNotificationConfigResponseDto;
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
public interface CreateChargePlanNotificationConfigWebMapper {

    CreateChargePlanNotificationConfigCommand toCommand(CreateChargePlanNotificationConfigRequestDto dto);
    Collection<CreateChargePlanNotificationConfigCommand> toCommand(Collection<CreateChargePlanNotificationConfigRequestDto> dtos);

    ChargePlanNotificationConfigResponseDto toDto(final ChargePlanNotificationConfig domain);
    Collection<ChargePlanNotificationConfigResponseDto> toDto(final Collection<ChargePlanNotificationConfig> domains);
}
