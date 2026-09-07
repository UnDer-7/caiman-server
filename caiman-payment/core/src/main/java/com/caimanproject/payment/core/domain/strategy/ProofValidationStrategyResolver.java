package com.caimanproject.payment.core.domain.strategy;

import com.caimanproject.payment.core.domain.types.ProofValidationMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProofValidationStrategyResolver {

    private final Map<ProofValidationMode, ProofValidationStrategy> byMode;

    public ProofValidationStrategyResolver(final List<ProofValidationStrategy> strategies) {
        this.byMode = strategies.stream()
                .collect(Collectors.toMap(ProofValidationStrategy::supportedMode, Function.identity()));
    }

    public ProofValidationStrategy resolve(final ProofValidationMode mode) {
        final var strategy = byMode.get(mode);
        if (strategy == null) {
            throw new IllegalStateException("No ProofValidationStrategy registered for mode " + mode);
        }
        return strategy;
    }
}
