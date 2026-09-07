package com.caimanproject.payment.infrastructure.database.adapter;

import com.caimanproject.payment.core.domain.types.PaymentProofStatus;
import com.caimanproject.payment.infrastructure.database.repository.PaymentProofRepository;
import com.caimanproject.test.annotation.UnitTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ProofQueryAdapterTest {

    @Mock
    PaymentProofRepository paymentProofRepository;

    @InjectMocks
    PaymentProofQueryAdapter adapter;

    @Test
    void should_return_true_when_a_non_rejected_proof_exists_for_invoice() {
        final var invoiceId = UUID.randomUUID();
        when(paymentProofRepository.existsByInvoiceIdAndStatusNot(invoiceId.toString(), PaymentProofStatus.REJECTED))
                .thenReturn(true);

        assertThat(adapter.existsActiveProof(invoiceId)).isTrue();
    }

    @Test
    void should_return_false_when_no_active_proof_exists_for_invoice() {
        final var invoiceId = UUID.randomUUID();
        when(paymentProofRepository.existsByInvoiceIdAndStatusNot(invoiceId.toString(), PaymentProofStatus.REJECTED))
                .thenReturn(false);

        assertThat(adapter.existsActiveProof(invoiceId)).isFalse();
    }
}
