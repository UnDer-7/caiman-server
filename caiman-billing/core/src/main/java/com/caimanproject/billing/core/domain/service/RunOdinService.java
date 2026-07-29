package com.caimanproject.billing.core.domain.service;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.port.in.RunOdinUseCase;
import com.caimanproject.billing.core.port.out.ChargePlanSearchGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunOdinService implements RunOdinUseCase {

    private final ChargePlanSearchGateway chargePlanSearchGateway;

    @Override
    public void execute() {
        final var today = LocalDate.now();
        final List<ChargePlan> dueChargePlans = chargePlanSearchGateway.getAllActives()
            .stream()
            .filter(cp -> cp.isGenerationDueOn(today))
            .toList();
        System.out.println("dueChargePlans: " + dueChargePlans.size());
    }

}
