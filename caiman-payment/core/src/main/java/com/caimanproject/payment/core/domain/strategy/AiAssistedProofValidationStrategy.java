package com.caimanproject.payment.core.domain.strategy;

import com.caimanproject.payment.core.domain.types.PaymentProofStatus;
import com.caimanproject.payment.core.domain.types.ProofValidationMode;
import org.springframework.stereotype.Component;

@Component
public class AiAssistedProofValidationStrategy implements ProofValidationStrategy {

    @Override
    public ProofValidationMode supportedMode() {
        return ProofValidationMode.AI_ASSISTED;
    }

    @Override
    public PaymentProofStatus resolveStatus(final ProofValidationContext context) {
        // TODO: implement AI_ASSISTED analysis (BUSINESS_RULES.md §8.3/§8.5) — async
        // Anthropic API call, isValid=true routes to PENDING_MANUAL_REVIEW for admin
        // confirmation, isValid=false routes to REJECTED. Not implemented in this spec.
        throw new UnsupportedOperationException("AI_ASSISTED proof validation is not implemented yet");
    }
}
