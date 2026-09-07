package com.caimanproject.payment.core.port.out;

import com.caimanproject.payment.core.domain.model.PaymentProof;

public interface PaymentProofPersistenceGateway {
    PaymentProof save(PaymentProof proof);
}
