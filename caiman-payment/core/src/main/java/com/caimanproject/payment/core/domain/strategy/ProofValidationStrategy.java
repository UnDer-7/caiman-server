package com.caimanproject.payment.core.domain.strategy;

import com.caimanproject.payment.core.domain.types.PaymentProofStatus;
import com.caimanproject.payment.core.domain.types.ProofValidationMode;

public interface ProofValidationStrategy {
    ProofValidationMode supportedMode();

    PaymentProofStatus resolveStatus(ProofValidationContext context);
}
