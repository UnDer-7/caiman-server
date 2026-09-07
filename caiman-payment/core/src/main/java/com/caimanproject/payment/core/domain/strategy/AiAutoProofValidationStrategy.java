package com.caimanproject.payment.core.domain.strategy;

import com.caimanproject.payment.core.domain.types.PaymentProofStatus;
import com.caimanproject.payment.core.domain.types.ProofValidationMode;
import org.springframework.stereotype.Component;

@Component
public class AiAutoProofValidationStrategy implements ProofValidationStrategy {

    @Override
    public ProofValidationMode supportedMode() {
        return ProofValidationMode.AI_AUTO;
    }

    @Override
    public PaymentProofStatus resolveStatus(final ProofValidationContext context) {
        // TODO: implement AI_AUTO analysis (BUSINESS_RULES.md §8.3/§8.4) — async Anthropic
        // API call, isValid routing to APPROVED/REJECTED, failure routing to
        // PENDING_MANUAL_REVIEW. Not implemented in this spec.
        throw new UnsupportedOperationException("AI_AUTO proof validation is not implemented yet");
    }
}
