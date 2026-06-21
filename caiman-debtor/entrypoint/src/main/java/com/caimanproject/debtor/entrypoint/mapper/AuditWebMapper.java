package com.caimanproject.debtor.entrypoint.mapper;

import com.caimanproject.contracts.util.Constants;
import com.caimanproject.debtor.core.domain.model.Audit;
import com.caimanproject.debtor.entrypoint.payload.response.AuditResponseDto;
import com.caimanproject.mapper.OptionalMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = OptionalMapper.class,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuditWebMapper {

    AuditResponseDto toDto(Audit model);
}
