package com.caimanproject.payment.core.port.out;

import java.util.UUID;

public interface ActiveProofExistsGateway {

    /**
     * @param invoiceId invoice to check
     * @return true if a payment_proof exists for this invoice with status NOT IN (REJECTED)
     */
    boolean existsActiveProof(UUID invoiceId);
}
