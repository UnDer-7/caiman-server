package com.caimanproject.payment.core.domain.strategy;

import com.caimanproject.payment.core.domain.types.PaymentProofStatus;
import com.caimanproject.payment.core.domain.types.ProofValidationMode;
import com.caimanproject.test.annotation.UnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
class ManualProofValidationStrategyTest {

    private final ManualProofValidationStrategy strategy = new ManualProofValidationStrategy();

    @Test
    void should_support_manual_mode() {
        assertThat(strategy.supportedMode()).isEqualTo(ProofValidationMode.MANUAL);
    }

    @Test
    void should_always_resolve_to_pending_manual_review() {
        final var context = new ProofValidationContext(new byte[0], "image/jpeg", "receipt.jpg", null, null, null);

        assertThat(strategy.resolveStatus(context)).isEqualTo(PaymentProofStatus.PENDING_MANUAL_REVIEW);
    }
}
