package com.caimanproject.payment.infrastructure.database.adapter;

import com.caimanproject.payment.core.domain.types.PaymentProofStatus;
import com.caimanproject.payment.core.port.out.PaymentProofQueryGateway;
import com.caimanproject.payment.infrastructure.database.repository.PaymentProofRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProofQueryAdapter implements PaymentProofQueryGateway {

    private final PaymentProofRepository paymentProofRepository;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveProof(final UUID invoiceId) {
        return paymentProofRepository.existsByInvoiceIdAndStatusNot(invoiceId.toString(), PaymentProofStatus.REJECTED);
    }
}
