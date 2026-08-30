package com.caimanproject.billing.infrastructure.producer.mapper;

import com.caimanproject.billing.core.domain.model.Invoice;
import com.caimanproject.contracts.event.InvoiceEventDto;
import com.caimanproject.contracts.util.Constants;
import com.caimanproject.mapper.OptionalMapper;
import java.time.Instant;
import java.util.UUID;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {OptionalMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InvoiceEventMapper {

    InvoiceEventDto toEventDto(
            Invoice invoice,
            UUID debtorId,
            String chargePlanName,
            Boolean invoiceCreatedNotificationEnabled,
            String uploadLink,
            Instant scheduledFor,
            Integer maxAttempts);
}
