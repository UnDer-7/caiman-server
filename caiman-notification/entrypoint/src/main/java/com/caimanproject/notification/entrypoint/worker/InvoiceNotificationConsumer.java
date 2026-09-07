package com.caimanproject.notification.entrypoint.worker;

import com.caimanproject.contracts.event.InvoiceEventDto;
import com.caimanproject.contracts.event.MessageEvent;
import com.caimanproject.notification.core.port.in.CreateInvoiceCreatedOutboxUseCase;
import com.caimanproject.notification.entrypoint.mapper.InvoiceEventCommandMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceNotificationConsumer {

    private final CreateInvoiceCreatedOutboxUseCase createInvoiceCreatedOutboxUseCase;
    private final InvoiceEventCommandMapper invoiceEventCommandMapper;

    @Async
    @EventListener
    void consumer(final MessageEvent<InvoiceEventDto> message) {
        log.info("Received InvoiceEvent {}", message);
        final var command = invoiceEventCommandMapper.toCommand(message.payload());
        createInvoiceCreatedOutboxUseCase.execute(command);
    }
}
