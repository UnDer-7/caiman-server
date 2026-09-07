package com.caimanproject.payment.core.domain.model;

import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.payment.core.domain.types.PaymentProofStatus;
import com.caimanproject.test.annotation.UnitTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class PaymentProofTest {

    @Test
    void should_create_proof_with_defaults_via_createBuilder() {
        final var invoiceId = UUID.randomUUID();

        final var proof = PaymentProof.createBuilder()
                .invoiceId(invoiceId)
                .filePath("some-file.jpg")
                .originalFilename("receipt.jpg")
                .fileContentType("image/jpeg")
                .fileSizeBytes(1024L)
                .uploadToken(UUID.randomUUID().toString())
                .status(PaymentProofStatus.PENDING_MANUAL_REVIEW)
                .requiresManualReview(true)
                .build();

        assertThat(proof.getId()).isEmpty();
        assertThat(proof.getInvoiceId()).isEqualTo(invoiceId);
        assertThat(proof.getStatus()).isEqualTo(PaymentProofStatus.PENDING_MANUAL_REVIEW);
        assertThat(proof.isRequiresManualReview()).isTrue();
        assertThat(proof.getAiExtractedValue()).isEmpty();
        assertThat(proof.getFinalValue()).isEmpty();
        assertThat(proof.getAiRawResponse()).isEmpty();
    }

    @Test
    void should_throw_domain_exception_when_invoiceId_is_missing() {
        assertThatThrownBy(() -> PaymentProof.restoreBuilder()
                        .invoiceId(null)
                        .filePath("some-file.jpg")
                        .originalFilename("receipt.jpg")
                        .fileContentType("image/jpeg")
                        .fileSizeBytes(1024L)
                        .uploadToken(UUID.randomUUID().toString())
                        .status(PaymentProofStatus.PENDING_MANUAL_REVIEW)
                        .build())
                .isInstanceOf(DomainException.class);
    }

    @Test
    void should_throw_domain_exception_when_status_is_missing() {
        assertThatThrownBy(() -> PaymentProof.restoreBuilder()
                        .invoiceId(UUID.randomUUID())
                        .filePath("some-file.jpg")
                        .originalFilename("receipt.jpg")
                        .fileContentType("image/jpeg")
                        .fileSizeBytes(1024L)
                        .uploadToken(UUID.randomUUID().toString())
                        .status(null)
                        .build())
                .isInstanceOf(DomainException.class);
    }
}
