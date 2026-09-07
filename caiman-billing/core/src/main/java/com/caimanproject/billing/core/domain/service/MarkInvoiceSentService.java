package com.caimanproject.billing.core.domain.service;

import com.caimanproject.billing.core.port.in.MarkInvoiceSentUseCase;
import com.caimanproject.billing.core.port.out.InvoicePersistenceGateway;
import com.caimanproject.billing.core.port.out.InvoiceSearchGateway;
import com.caimanproject.contracts.event.NotificationSentEventDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarkInvoiceSentService implements MarkInvoiceSentUseCase {

    private final InvoiceSearchGateway invoiceSearchGateway;
    private final InvoicePersistenceGateway invoicePersistenceGateway;

    @Override
    public void execute(final UUID invoiceId, final String triggerType) {
        if (!NotificationSentEventDto.TRIGGER_TYPE_INVOICE_CREATED.equals(triggerType)) {
            return;
        }

        invoiceSearchGateway
                .findById(invoiceId)
                .ifPresentOrElse(
                        invoice -> invoicePersistenceGateway.save(invoice.markSent()),
                        () -> log.warn("invoice {} not found for notification-sent event, skipping", invoiceId));
    }
}
