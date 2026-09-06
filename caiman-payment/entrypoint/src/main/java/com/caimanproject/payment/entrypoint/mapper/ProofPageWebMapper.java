package com.caimanproject.payment.entrypoint.mapper;

import com.caimanproject.contracts.util.Constants;
import com.caimanproject.payment.core.domain.model.ProofPageView;
import com.caimanproject.payment.entrypoint.payload.response.ProofPageResponseDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProofPageWebMapper {

    ProofPageResponseDto toDto(ProofPageView view);
}
