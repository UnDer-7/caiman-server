package com.caimanproject.payment.infrastructure.database.mapper;

import com.caimanproject.contracts.util.Constants;
import com.caimanproject.mapper.IdMapper;
import com.caimanproject.mapper.OptionalMapper;
import com.caimanproject.payment.core.domain.model.PaymentProof;
import com.caimanproject.payment.infrastructure.database.entity.PaymentProofEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {OptionalMapper.class, IdMapper.class, PaymentAuditEntityMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentProofMapper {

    PaymentProof toModel(PaymentProofEntity entity);

    PaymentProofEntity toEntity(PaymentProof model);
}
