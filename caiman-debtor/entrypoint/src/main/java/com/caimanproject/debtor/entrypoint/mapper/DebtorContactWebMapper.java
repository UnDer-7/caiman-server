package com.caimanproject.debtor.entrypoint.mapper;

import com.caimanproject.contracts.util.Constants;
import com.caimanproject.debtor.core.domain.model.DebtorContact;
import com.caimanproject.debtor.core.port.in.command.CreateDebtorContactCommand;
import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorContactRequestDto;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorContactResponseDto;
import com.caimanproject.mapper.OptionalMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    uses = {OptionalMapper.class, AuditWebMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DebtorContactWebMapper {

    CreateDebtorContactCommand toCommand(CreateDebtorContactRequestDto dto);

    DebtorContactResponseDto toDto(final DebtorContact domain);
}
