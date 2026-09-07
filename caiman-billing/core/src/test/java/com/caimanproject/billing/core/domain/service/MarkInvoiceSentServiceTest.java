package com.caimanproject.billing.core.domain.service;

import com.caimanproject.billing.core.domain.model.Invoice;
import com.caimanproject.billing.core.domain.types.InvoiceStatus;
import com.caimanproject.billing.core.port.out.InvoicePersistenceGateway;
import com.caimanproject.billing.core.port.out.InvoiceSearchGateway;
import com.caimanproject.contracts.event.NotificationSentEventDto;
import com.caimanproject.test.annotation.UnitTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class MarkInvoiceSentServiceTest {

    @Mock
    InvoiceSearchGateway invoiceSearchGateway;

    @Mock
    InvoicePersistenceGateway invoicePersistenceGateway;

    @InjectMocks
    MarkInvoiceSentService service;

    @Test
    void marks_pending_invoice_as_sent_when_trigger_is_invoice_created() {
        // Given
        final var invoice = buildPendingInvoice();
        Mockito.when(invoiceSearchGateway.findById(invoice.getId().orElseThrow()))
                .thenReturn(Optional.of(invoice));
        Mockito.when(invoicePersistenceGateway.save(Mockito.any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        service.execute(invoice.getId().orElseThrow(), NotificationSentEventDto.TRIGGER_TYPE_INVOICE_CREATED);

        // Then
        final var captor = ArgumentCaptor.forClass(Invoice.class);
        Mockito.verify(invoicePersistenceGateway).save(captor.capture());
        Assertions.assertThat(captor.getValue().getStatus()).isEqualTo(InvoiceStatus.SENT);
    }

    @Test
    void ignores_trigger_types_other_than_invoice_created() {
        // When
        service.execute(UUID.randomUUID(), "PENDING_REMINDER");

        // Then
        Mockito.verifyNoInteractions(invoiceSearchGateway, invoicePersistenceGateway);
    }

    @Test
    void does_not_fail_when_invoice_is_not_found() {
        // Given
        final var invoiceId = UUID.randomUUID();
        Mockito.when(invoiceSearchGateway.findById(invoiceId)).thenReturn(Optional.empty());

        // When
        service.execute(invoiceId, NotificationSentEventDto.TRIGGER_TYPE_INVOICE_CREATED);

        // Then
        Mockito.verify(invoicePersistenceGateway, Mockito.never()).save(Mockito.any());
    }

    private static Invoice buildPendingInvoice() {
        return Invoice.restoreBuilder()
                .id(UUID.randomUUID())
                .chargePlanId(UUID.randomUUID())
                .chargePlanMemberId(UUID.randomUUID())
                .cycleIndex(1L)
                .generationDate(LocalDate.now())
                .amountDue(new BigDecimal("10.00"))
                .amountPaid(BigDecimal.ZERO)
                .status(InvoiceStatus.PENDING)
                .dueDate(Instant.now())
                .uploadToken(UUID.randomUUID())
                .build();
    }
}
