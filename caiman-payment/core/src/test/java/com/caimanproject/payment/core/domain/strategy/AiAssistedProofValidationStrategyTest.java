package com.caimanproject.payment.core.domain.strategy;

import com.caimanproject.payment.core.domain.types.ProofValidationMode;
import com.caimanproject.test.annotation.UnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class AiAssistedProofValidationStrategyTest {

    private final AiAssistedProofValidationStrategy strategy = new AiAssistedProofValidationStrategy();

    @Test
    void should_support_ai_assisted_mode() {
        assertThat(strategy.supportedMode()).isEqualTo(ProofValidationMode.AI_ASSISTED);
    }

    @Test
    void should_throw_unsupported_operation_exception() {
        final var context = new ProofValidationContext(new byte[0], "image/jpeg", "receipt.jpg", null, null, null);

        assertThatThrownBy(() -> strategy.resolveStatus(context)).isInstanceOf(UnsupportedOperationException.class);
    }
}
