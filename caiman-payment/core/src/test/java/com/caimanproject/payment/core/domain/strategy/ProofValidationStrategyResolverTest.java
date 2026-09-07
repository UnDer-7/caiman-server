package com.caimanproject.payment.core.domain.strategy;

import com.caimanproject.payment.core.domain.types.ProofValidationMode;
import com.caimanproject.test.annotation.UnitTest;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UnitTest
class ProofValidationStrategyResolverTest {

    private final ManualProofValidationStrategy manualStrategy = new ManualProofValidationStrategy();
    private final AiAutoProofValidationStrategy aiAutoStrategy = new AiAutoProofValidationStrategy();
    private final AiAssistedProofValidationStrategy aiAssistedStrategy = new AiAssistedProofValidationStrategy();

    private final ProofValidationStrategyResolver resolver =
            new ProofValidationStrategyResolver(List.of(manualStrategy, aiAutoStrategy, aiAssistedStrategy));

    @Test
    void should_resolve_each_mode_to_the_matching_strategy() {
        assertThat(resolver.resolve(ProofValidationMode.MANUAL)).isSameAs(manualStrategy);
        assertThat(resolver.resolve(ProofValidationMode.AI_AUTO)).isSameAs(aiAutoStrategy);
        assertThat(resolver.resolve(ProofValidationMode.AI_ASSISTED)).isSameAs(aiAssistedStrategy);
    }

    @Test
    void should_throw_when_mode_has_no_registered_strategy() {
        final var resolverWithoutManual =
                new ProofValidationStrategyResolver(List.of(aiAutoStrategy, aiAssistedStrategy));

        assertThatThrownBy(() -> resolverWithoutManual.resolve(ProofValidationMode.MANUAL))
                .isInstanceOf(IllegalStateException.class);
    }
}
