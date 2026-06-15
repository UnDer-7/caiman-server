package com.caimanproject.debtor.entrypoint.mapper;

import com.caimanproject.contracts.util.Constants;
import com.caimanproject.debtor.core.domain.model.Debtor;
import com.caimanproject.debtor.core.port.in.command.CreateDebtorCommand;
import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorRequestDto;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorResponseDto;
import com.caimanproject.mapper.OptionalMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        uses = {OptionalMapper.class, DebtorContactWebMapper.class, AuditWebMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DebtorWebMapper {

    CreateDebtorCommand toCommand(CreateDebtorRequestDto dto);

    DebtorResponseDto toDto(Debtor domain);
}
