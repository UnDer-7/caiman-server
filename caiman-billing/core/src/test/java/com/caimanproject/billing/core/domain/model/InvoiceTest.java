package com.caimanproject.billing.core.domain.model;

import com.caimanproject.billing.core.domain.types.InvoiceStatus;
import com.caimanproject.test.annotation.UnitTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@UnitTest
class InvoiceTest {

    @Test
    void markSent_transitions_pending_to_sent() {
        // Given
        final var invoice = buildPendingInvoice();

        // When
        final var result = invoice.markSent();

        // Then
        Assertions.assertThat(result.getStatus()).isEqualTo(InvoiceStatus.SENT);
        Assertions.assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    void markSent_is_noop_when_status_is_not_pending() {
        // Given
        final var invoice = buildPendingInvoice().markSent();
        Assertions.assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SENT);

        // When
        final var result = invoice.markSent();

        // Then
        Assertions.assertThat(result).isSameAs(invoice);
        Assertions.assertThat(result.getStatus()).isEqualTo(InvoiceStatus.SENT);
    }

    private static Invoice buildPendingInvoice() {
        return Invoice.createBuilder()
                .chargePlanId(UUID.randomUUID())
                .chargePlanMemberId(UUID.randomUUID())
                .cycleIndex(1L)
                .generationDate(LocalDate.now())
                .amountDue(new BigDecimal("10.00"))
                .dueDate(Instant.now())
                .build();
    }
}
