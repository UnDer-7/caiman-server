package com.caimanproject.notification.entrypoint.worker;

import com.caimanproject.contracts.event.InvoiceEventDto;
import com.caimanproject.contracts.event.MessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceNotificationConsumer {

    @Async
    @EventListener
    void consumer(final MessageEvent<InvoiceEventDto> message) throws InterruptedException {
        log.info("Received InvoiceEvent {}", message);
        Thread.sleep(6000);
    }
}
