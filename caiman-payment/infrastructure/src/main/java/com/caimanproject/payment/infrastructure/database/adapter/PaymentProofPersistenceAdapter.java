package com.caimanproject.payment.infrastructure.database.adapter;

import com.caimanproject.payment.core.domain.model.PaymentProof;
import com.caimanproject.payment.core.port.out.PaymentProofPersistenceGateway;
import com.caimanproject.payment.infrastructure.database.mapper.PaymentProofMapper;
import com.caimanproject.payment.infrastructure.database.repository.PaymentProofRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProofPersistenceAdapter implements PaymentProofPersistenceGateway {

    private final PaymentProofRepository paymentProofRepository;
    private final PaymentProofMapper paymentProofMapper;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public PaymentProof save(final PaymentProof proof) {
        final var entity = paymentProofMapper.toEntity(proof);
        final var savedEntity = paymentProofRepository.save(entity);
        return paymentProofMapper.toModel(savedEntity);
    }
}
