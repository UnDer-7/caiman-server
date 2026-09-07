package com.caimanproject.notification.entrypoint.mapper;

import com.caimanproject.contracts.event.InvoiceEventDto;
import com.caimanproject.contracts.util.Constants;
import com.caimanproject.notification.core.port.in.command.CreateInvoiceCreatedOutboxCommand;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InvoiceEventCommandMapper {

    @Mapping(target = "invoiceId", source = "id")
    @Mapping(target = "planName", source = "chargePlanName")
    CreateInvoiceCreatedOutboxCommand toCommand(InvoiceEventDto dto);
}
