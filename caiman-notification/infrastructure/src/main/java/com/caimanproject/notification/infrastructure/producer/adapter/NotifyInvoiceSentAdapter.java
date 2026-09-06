package com.caimanproject.notification.infrastructure.producer.adapter;

import com.caimanproject.contracts.event.MessageEvent;
import com.caimanproject.contracts.event.NotificationSentEventDto;
import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import com.caimanproject.notification.core.port.out.NotifyInvoiceSentGateway;
import com.caimanproject.notification.infrastructure.producer.mapper.NotificationSentEventMapper;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class NotifyInvoiceSentAdapter implements NotifyInvoiceSentGateway {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final NotificationSentEventMapper notificationSentEventMapper;

    @Override
    public void notify(final NotificationOutbox outbox, final Instant sentAt) {
        final NotificationSentEventDto payload = notificationSentEventMapper.toEventDto(outbox, sentAt);
        final MessageEvent.Metadata metadata = new MessageEvent.Metadata(
                UUID.randomUUID(),
                NotificationSentEventDto.EVENT_TYPE_NOTIFICATION_SENT,
                outbox.getInvoiceId() + ":" + outbox.getTriggerType());

        final var message = new MessageEvent<>(metadata, payload);

        // MessageEvent<T> erases T at runtime, so publishEvent(Object) alone resolves the
        // wrapped PayloadApplicationEvent's generic to raw MessageEvent. A listener declared
        // as @EventListener(MessageEvent<NotificationSentEventDto>) then silently never matches.
        // Passing the ResolvableType explicitly restores generic-based dispatch.
        final ResolvableType eventType =
                ResolvableType.forClassWithGenerics(MessageEvent.class, NotificationSentEventDto.class);
        applicationEventPublisher.publishEvent(new PayloadApplicationEvent<>(this, message, eventType));

        log.info(
                "notification sent event published for invoiceId={} triggerType={} correlationId={}",
                outbox.getInvoiceId(),
                outbox.getTriggerType(),
                metadata.correlationId());
    }
}
