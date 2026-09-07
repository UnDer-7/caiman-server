package com.caimanproject.billing.core.domain.service.odin;

import com.caimanproject.billing.core.port.in.RunOdinUseCase;
import com.caimanproject.billing.core.port.out.ChargePlanSearchGateway;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RunOdinService implements RunOdinUseCase {

    private final ChargePlanSearchGateway chargePlanSearchGateway;
    private final RotatingInvoiceGenerator rotatingInvoiceGenerator;
    private final SplitInvoiceGenerator splitInvoiceGenerator;

    @Override
    public void execute() {
        final var today = LocalDate.now(ZoneOffset.UTC);
        chargePlanSearchGateway.getAllActives().stream()
                .filter(cp -> cp.isGenerationDueOn(today))
                .forEach(cp -> {
                    switch (cp.getType()) {
                        case ROTATING -> rotatingInvoiceGenerator.generate(cp, today);
                        case SPLIT -> splitInvoiceGenerator.generate(cp, today);
                    }
                });
    }
}
