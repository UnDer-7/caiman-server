package com.caimanproject.payment.infrastructure.database.adapter;

import com.caimanproject.payment.core.port.out.ActiveProofExistsGateway;
import com.caimanproject.payment.infrastructure.database.entity.PaymentProofStatus;
import com.caimanproject.payment.infrastructure.database.repository.PaymentProofRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveProofExistsAdapter implements ActiveProofExistsGateway {

    private final PaymentProofRepository paymentProofRepository;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveProof(final UUID invoiceId) {
        return paymentProofRepository.existsByInvoiceIdAndStatusNot(invoiceId.toString(), PaymentProofStatus.REJECTED);
    }
}
