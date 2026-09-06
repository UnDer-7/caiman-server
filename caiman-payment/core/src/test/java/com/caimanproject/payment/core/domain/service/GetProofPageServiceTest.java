package com.caimanproject.payment.core.domain.service;

import com.caimanproject.contracts.exception.NotFoundException;
import com.caimanproject.contracts.gateway.invoice.InvoiceGateway;
import com.caimanproject.contracts.gateway.invoice.InvoiceSnapshotDto;
import com.caimanproject.payment.core.port.in.command.GetProofPageCommand;
import com.caimanproject.payment.core.port.out.ActiveProofExistsGateway;
import com.caimanproject.test.annotation.UnitTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
class GetProofPageServiceTest {

    @Mock
    InvoiceGateway invoiceGateway;

    @Mock
    ActiveProofExistsGateway activeProofExistsGateway;

    @InjectMocks
    GetProofPageService service;

    @Test
    void should_throw_not_found_when_token_does_not_match_any_invoice() {
        final var token = UUID.randomUUID();
        when(invoiceGateway.findByUploadToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new GetProofPageCommand(token)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_enable_form_when_invoice_is_pending_and_no_active_proof() {
        final var token = UUID.randomUUID();
        final var invoiceId = UUID.randomUUID();
        final var dueDate = Instant.now();
        final var snapshot = InvoiceSnapshotDto.builder()
                .id(invoiceId)
                .chargePlanName("Shared Plan")
                .debtorName("John Doe")
                .amountDue(new BigDecimal("100.00"))
                .amountPaid(BigDecimal.ZERO)
                .status("PENDING")
                .dueDate(dueDate)
                .cycleIndex(2)
                .build();
        when(invoiceGateway.findByUploadToken(token)).thenReturn(Optional.of(snapshot));
        when(activeProofExistsGateway.existsActiveProof(invoiceId)).thenReturn(false);

        final var view = service.execute(new GetProofPageCommand(token));

        assertThat(view.formEnabled()).isTrue();
        assertThat(view.statusMessage()).isNull();
        assertThat(view.chargePlanName()).isEqualTo("Shared Plan");
        assertThat(view.debtorName()).isEqualTo("John Doe");
        assertThat(view.amountDue()).isEqualByComparingTo("100.00");
        assertThat(view.amountPaid()).isEqualByComparingTo("0");
        assertThat(view.dueDate()).isEqualTo(dueDate);
        assertThat(view.cycleIndex()).isEqualTo(2);
    }

    @Test
    void should_disable_form_when_invoice_is_paid() {
        final var token = UUID.randomUUID();
        final var invoiceId = UUID.randomUUID();
        final var snapshot = InvoiceSnapshotDto.builder()
                .id(invoiceId)
                .amountDue(BigDecimal.TEN)
                .amountPaid(BigDecimal.TEN)
                .status("PAID")
                .dueDate(Instant.now())
                .cycleIndex(1)
                .build();
        when(invoiceGateway.findByUploadToken(token)).thenReturn(Optional.of(snapshot));

        final var view = service.execute(new GetProofPageCommand(token));

        assertThat(view.formEnabled()).isFalse();
        assertThat(view.statusMessage()).isEqualTo("This invoice has already been paid.");
    }

    @Test
    void should_disable_form_when_invoice_is_cancelled() {
        final var token = UUID.randomUUID();
        final var invoiceId = UUID.randomUUID();
        final var snapshot = InvoiceSnapshotDto.builder()
                .id(invoiceId)
                .amountDue(BigDecimal.TEN)
                .amountPaid(BigDecimal.ZERO)
                .status("CANCELLED")
                .dueDate(Instant.now())
                .cycleIndex(1)
                .build();
        when(invoiceGateway.findByUploadToken(token)).thenReturn(Optional.of(snapshot));

        final var view = service.execute(new GetProofPageCommand(token));

        assertThat(view.formEnabled()).isFalse();
        assertThat(view.statusMessage()).isEqualTo("This invoice has been cancelled.");
    }

    @Test
    void should_disable_form_when_active_proof_already_exists() {
        final var token = UUID.randomUUID();
        final var invoiceId = UUID.randomUUID();
        final var snapshot = InvoiceSnapshotDto.builder()
                .id(invoiceId)
                .amountDue(BigDecimal.TEN)
                .amountPaid(BigDecimal.ZERO)
                .status("SENT")
                .dueDate(Instant.now())
                .cycleIndex(1)
                .build();
        when(invoiceGateway.findByUploadToken(token)).thenReturn(Optional.of(snapshot));
        when(activeProofExistsGateway.existsActiveProof(invoiceId)).thenReturn(true);

        final var view = service.execute(new GetProofPageCommand(token));

        assertThat(view.formEnabled()).isFalse();
        assertThat(view.statusMessage()).isEqualTo("A proof is already under review for this invoice.");
    }
}
