package com.caimanproject.payment.core.domain.strategy;

import com.caimanproject.payment.core.domain.types.PaymentProofStatus;
import com.caimanproject.payment.core.domain.types.ProofValidationMode;
import org.springframework.stereotype.Component;

@Component
public class ManualProofValidationStrategy implements ProofValidationStrategy {

    @Override
    public ProofValidationMode supportedMode() {
        return ProofValidationMode.MANUAL;
    }

    @Override
    public PaymentProofStatus resolveStatus(final ProofValidationContext context) {
        // BUSINESS_RULES.md §8.2 — MANUAL always goes straight to admin review,
        // no analysis step, regardless of file/amount/anything else.
        return PaymentProofStatus.PENDING_MANUAL_REVIEW;
    }
}
