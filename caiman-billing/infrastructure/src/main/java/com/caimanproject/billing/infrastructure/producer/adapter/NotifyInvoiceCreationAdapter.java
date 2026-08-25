package com.caimanproject.billing.infrastructure.producer.adapter;

import com.caimanproject.billing.core.domain.model.Invoice;
import com.caimanproject.billing.core.port.out.NotifyInvoiceCreationGateway;
import com.caimanproject.billing.infrastructure.producer.mapper.InvoiceEventMapper;
import com.caimanproject.contracts.event.InvoiceEventDto;
import com.caimanproject.contracts.event.MessageEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class NotifyInvoiceCreationAdapter implements NotifyInvoiceCreationGateway {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final InvoiceEventMapper invoiceEventMapper;

    @Override
    public void notify(
            final Invoice invoice, final String chargePlanName, final Instant scheduledFor, final int maxAttempts) {
        final InvoiceEventDto payload =
                invoiceEventMapper.toEventDto(invoice, chargePlanName, scheduledFor, maxAttempts);
        final MessageEvent.Metadata metadata = buildMetadata(invoice);

        final var message = new MessageEvent<>(metadata, payload);

        applicationEventPublisher.publishEvent(message);
    }

    private MessageEvent.Metadata buildMetadata(final Invoice invoice) {
        return new MessageEvent.Metadata(
                UUID.randomUUID(), // todo: ver depois como pegar
                InvoiceEventDto.EVENT_TYPE_GENERATED,
                invoice.getId().orElseThrow().toString());
    }
}
