package com.caimanproject.billing.infrastructure.producer.adapter;

import com.caimanproject.billing.core.domain.model.Invoice;
import com.caimanproject.billing.core.port.out.NotifyInvoiceCreationGateway;
import com.caimanproject.billing.infrastructure.producer.mapper.InvoiceEventMapper;
import com.caimanproject.contracts.event.InvoiceEventDto;
import com.caimanproject.contracts.event.MessageEvent;
import com.caimanproject.contracts.exception.LogField;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class NotifyInvoiceCreationAdapter implements NotifyInvoiceCreationGateway {

    // TODO: replace with a configured public base URL once the proof-upload endpoint exists (caiman-payment).
    private static final String UPLOAD_LINK_BASE = "https://caiman.example.com/upload-invoice/";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final InvoiceEventMapper invoiceEventMapper;

    @Override
    public void notify(
            final Invoice invoice,
            final UUID debtorId,
            final String chargePlanName,
            final boolean invoiceCreatedNotificationEnabled,
            final Instant scheduledFor,
            final int maxAttempts) {
        final String uploadLink = UPLOAD_LINK_BASE + invoice.getUploadToken();
        final InvoiceEventDto payload = invoiceEventMapper.toEventDto(
                invoice,
                debtorId,
                chargePlanName,
                invoiceCreatedNotificationEnabled,
                uploadLink,
                scheduledFor,
                maxAttempts);
        final MessageEvent.Metadata metadata = buildMetadata(invoice);

        final var message = new MessageEvent<>(metadata, payload);

        // MessageEvent<T> erases T at runtime, so publishEvent(Object) alone resolves the
        // wrapped PayloadApplicationEvent's generic to raw MessageEvent. A listener declared
        // as @EventListener(MessageEvent<InvoiceEventDto>) then silently never matches.
        // Passing the ResolvableType explicitly restores generic-based dispatch.
        // todo: mover para lugar generico
        final ResolvableType eventType = ResolvableType.forClassWithGenerics(MessageEvent.class, InvoiceEventDto.class);
        applicationEventPublisher.publishEvent(new PayloadApplicationEvent<>(this, message, eventType));

        log.info(
                LogField.Placeholders.FOUR.getPlaceholder(),
                StructuredArguments.kv(LogField.MSG.label(), "invoice creation notification event published"),
                StructuredArguments.kv(LogField.CHARGE_PLAN_NAME.label(), chargePlanName),
                StructuredArguments.kv(LogField.INVOICE_ID.label(), invoice.getId().orElseThrow()),
                StructuredArguments.kv(LogField.CORRELATION_ID.label(), metadata.correlationId()));
    }

    private MessageEvent.Metadata buildMetadata(final Invoice invoice) {
        return new MessageEvent.Metadata(
                UUID.randomUUID(), // todo: ver depois como pegar
                InvoiceEventDto.EVENT_TYPE_GENERATED,
                invoice.getId().orElseThrow().toString());
    }
}
