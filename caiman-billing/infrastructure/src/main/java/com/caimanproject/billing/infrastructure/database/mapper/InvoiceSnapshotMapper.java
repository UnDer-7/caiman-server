package com.caimanproject.billing.infrastructure.database.mapper;

import com.caimanproject.billing.infrastructure.database.entity.InvoiceEntity;
import com.caimanproject.contracts.gateway.invoice.InvoiceSnapshotDto;
import com.caimanproject.contracts.util.Constants;
import com.caimanproject.mapper.IdMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {IdMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InvoiceSnapshotMapper {

    @Mapping(target = "chargePlanName", source = "chargePlan.name")
    @Mapping(target = "debtorName", ignore = true)
    InvoiceSnapshotDto toSnapshotDto(InvoiceEntity entity);
}
