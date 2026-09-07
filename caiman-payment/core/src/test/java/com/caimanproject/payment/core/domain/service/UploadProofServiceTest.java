package com.caimanproject.payment.core.domain.service;

import com.caimanproject.contracts.exception.BusinessException;
import com.caimanproject.contracts.exception.NotFoundException;
import com.caimanproject.contracts.gateway.invoice.InvoiceGateway;
import com.caimanproject.contracts.gateway.invoice.InvoiceSnapshotDto;
import com.caimanproject.payment.core.domain.model.PaymentProof;
import com.caimanproject.payment.core.domain.strategy.ManualProofValidationStrategy;
import com.caimanproject.payment.core.domain.strategy.ProofValidationStrategyResolver;
import com.caimanproject.payment.core.domain.types.BusinessExceptionCode;
import com.caimanproject.payment.core.domain.types.PaymentProofStatus;
import com.caimanproject.payment.core.domain.types.PaymentType;
import com.caimanproject.payment.core.port.in.command.UploadProofCommand;
import com.caimanproject.payment.core.port.out.PaymentProofPersistenceGateway;
import com.caimanproject.payment.core.port.out.PaymentProofQueryGateway;
import com.caimanproject.payment.core.port.out.ProofFileStorageGateway;
import com.caimanproject.test.annotation.UnitTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UploadProofServiceTest {

    @Mock
    InvoiceGateway invoiceGateway;

    @Mock
    PaymentProofQueryGateway paymentProofQueryGateway;

    @Mock
    ProofFileStorageGateway proofFileStorageGateway;

    @Mock
    PaymentProofPersistenceGateway paymentProofPersistenceGateway;

    ProofValidationStrategyResolver strategyResolver;

    UploadProofService service;

    @BeforeEach
    void setUp() {
        strategyResolver = new ProofValidationStrategyResolver(List.of(new ManualProofValidationStrategy()));

        service = new UploadProofService(
                invoiceGateway,
                paymentProofQueryGateway,
                strategyResolver,
                proofFileStorageGateway,
                paymentProofPersistenceGateway);
    }

    private UploadProofCommand command(final UUID token) {
        return new UploadProofCommand(
                token, "fake-bytes".getBytes(), "image/jpeg", "receipt.jpg", PaymentType.TOTAL, null);
    }

    private InvoiceSnapshotDto invoiceSnapshot(final UUID invoiceId, final String status) {
        return InvoiceSnapshotDto.builder()
                .id(invoiceId)
                .chargePlanName("Shared Plan")
                .debtorName("John Doe")
                .amountDue(new BigDecimal("100.00"))
                .amountPaid(BigDecimal.ZERO)
                .status(status)
                .proofValidationMode("MANUAL")
                .cycleIndex(1)
                .build();
    }

    @Test
    void should_throw_not_found_when_token_does_not_match_any_invoice() {
        final var token = UUID.randomUUID();
        when(invoiceGateway.findByUploadToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(command(token))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throw_business_exception_when_active_proof_already_exists() {
        final var token = UUID.randomUUID();
        final var invoiceId = UUID.randomUUID();
        when(invoiceGateway.findByUploadToken(token)).thenReturn(Optional.of(invoiceSnapshot(invoiceId, "SENT")));
        when(paymentProofQueryGateway.existsActiveProof(invoiceId)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(command(token)))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                                .getErrors()
                                .get(0)
                                .getCode())
                        .isEqualTo(BusinessExceptionCode.ACTIVE_PROOF_EXISTS));
    }

    @Test
    void should_throw_business_exception_when_invoice_is_paid() {
        final var token = UUID.randomUUID();
        final var invoiceId = UUID.randomUUID();
        when(invoiceGateway.findByUploadToken(token)).thenReturn(Optional.of(invoiceSnapshot(invoiceId, "PAID")));
        when(paymentProofQueryGateway.existsActiveProof(invoiceId)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(command(token)))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                                .getErrors()
                                .get(0)
                                .getCode())
                        .isEqualTo(BusinessExceptionCode.INVOICE_NOT_PAYABLE));
    }

    @Test
    void should_throw_business_exception_when_invoice_is_cancelled() {
        final var token = UUID.randomUUID();
        final var invoiceId = UUID.randomUUID();
        when(invoiceGateway.findByUploadToken(token)).thenReturn(Optional.of(invoiceSnapshot(invoiceId, "CANCELLED")));
        when(paymentProofQueryGateway.existsActiveProof(invoiceId)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(command(token)))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                                .getErrors()
                                .get(0)
                                .getCode())
                        .isEqualTo(BusinessExceptionCode.INVOICE_NOT_PAYABLE));
    }

    @Test
    void should_persist_proof_as_pending_manual_review_for_manual_mode() {
        final var token = UUID.randomUUID();
        final var invoiceId = UUID.randomUUID();
        when(invoiceGateway.findByUploadToken(token)).thenReturn(Optional.of(invoiceSnapshot(invoiceId, "SENT")));
        when(paymentProofQueryGateway.existsActiveProof(invoiceId)).thenReturn(false);
        when(proofFileStorageGateway.store(any())).thenReturn("some/file/path.jpg");
        when(paymentProofPersistenceGateway.save(any())).thenAnswer(invocation -> {
            final PaymentProof proof = invocation.getArgument(0);
            return PaymentProof.restoreBuilder()
                    .id(UUID.randomUUID())
                    .invoiceId(proof.getInvoiceId())
                    .filePath(proof.getFilePath())
                    .originalFilename(proof.getOriginalFilename())
                    .fileContentType(proof.getFileContentType())
                    .fileSizeBytes(proof.getFileSizeBytes())
                    .uploadToken(proof.getUploadToken())
                    .requiresManualReview(proof.isRequiresManualReview())
                    .status(proof.getStatus())
                    .build();
        });

        final var result = service.execute(command(token));

        assertThat(result.status()).isEqualTo(PaymentProofStatus.PENDING_MANUAL_REVIEW);
        assertThat(result.proofId()).isNotNull();
    }
}
