package com.caimanproject.billing.infrastructure.database.mapper;

import com.caimanproject.billing.core.domain.model.Invoice;
import com.caimanproject.billing.infrastructure.database.entity.InvoiceEntity;
import com.caimanproject.contracts.util.Constants;
import com.caimanproject.mapper.IdMapper;
import com.caimanproject.mapper.OptionalMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {OptionalMapper.class, IdMapper.class, BillingAuditEntityMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InvoiceEntityMapper {

    @Mapping(target = "chargePlanId", source = "chargePlan.id")
    @Mapping(target = "chargePlanMemberId", source = "chargePlanMember.id")
    Invoice toModel(InvoiceEntity entity);

    @Mapping(target = "chargePlan", ignore = true)
    @Mapping(target = "chargePlanMember", ignore = true)
    InvoiceEntity toEntity(Invoice model);
}
