package com.caimanproject.debtor.entrypoint.mapper;

import com.caimanproject.contracts.util.Constants;
import com.caimanproject.debtor.core.domain.model.Debtor;
import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorRequestDto;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
    uses = {OptionalWebMapper.class, DebtorContactWebMapper.class, AuditWebMapper.class},
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DebtorWebMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "audit", ignore = true)
    Debtor toModel(CreateDebtorRequestDto dto);

    DebtorResponseDto toDto(Debtor domain);
}
