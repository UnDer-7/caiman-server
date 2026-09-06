package com.caimanproject.billing.entrypoint.worker;

import com.caimanproject.billing.core.port.in.MarkInvoiceSentUseCase;
import com.caimanproject.contracts.event.MessageEvent;
import com.caimanproject.contracts.event.NotificationSentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSentEventListener {

    private final MarkInvoiceSentUseCase markInvoiceSentUseCase;

    @Async
    @EventListener
    void consume(final MessageEvent<NotificationSentEventDto> message) {
        log.info("Received NotificationSentEvent {}", message);
        final var payload = message.payload();
        markInvoiceSentUseCase.execute(payload.invoiceId(), payload.triggerType());
    }
}
