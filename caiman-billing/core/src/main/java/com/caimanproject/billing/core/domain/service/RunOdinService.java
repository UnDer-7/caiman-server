package com.caimanproject.billing.core.domain.service;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.model.ChargePlanMember;
import com.caimanproject.billing.core.domain.model.Invoice;
import com.caimanproject.billing.core.port.in.RunOdinUseCase;
import com.caimanproject.billing.core.port.out.ChargePlanSearchGateway;
import com.caimanproject.billing.core.port.out.InvoicePersistenceGateway;
import com.caimanproject.billing.core.port.out.InvoiceSearchGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunOdinService implements RunOdinUseCase {

    private final ChargePlanSearchGateway chargePlanSearchGateway;
    private final InvoiceSearchGateway invoiceSearchGateway;
    private final InvoicePersistenceGateway invoicePersistenceGateway;

    @Override
    public void execute() {
        // todo: ver se ja gerou um invoice para o dia (ou semana ou mes) vigente, para nao gerar dois invoices
        final var today = LocalDate.now();
        chargePlanSearchGateway.getAllActives()
            .stream()
            .filter(cp -> cp.isGenerationDueOn(today))
            .forEach(cp -> {
                switch (cp.getType()) {
                    case ROTATING -> processRotating(cp);
                    case SPLIT -> processSplit(cp);
                };
            });
    }

    private void processRotating(final ChargePlan chargePlan) {
        final List<ChargePlanMember> activeMembers = chargePlan.getActiveMembers()
            .stream()
            .sorted(Comparator.comparingInt(m -> m.getRotationOrder().orElseThrow())) // todo: jogar exception
            .toList();

        if (activeMembers.isEmpty()) {
            // todo: colocar log
            return;
        }

        final UUID chargePlanId = chargePlan.getId().orElseThrow();
        final Long currentCycleIndex = invoiceSearchGateway.findMaxCycleIndex(chargePlanId)
            .map(ci -> ci + 1)
            .orElse(0L);
        final int currentRotationOrder = currentCycleIndex.intValue() % activeMembers.size();
        final ChargePlanMember currentMember = activeMembers.get(currentRotationOrder); // todo validar se nao encontrar jogar exception
        final BigDecimal dueAmount = currentMember.getDueAmount(chargePlan.getTotalAmount()); // todo: debitar o credito usado, talvez deixar na mesma funcao q retonar o valor ja debita
        final Invoice invoice = Invoice.createBuilder()
            .chargePlanId(chargePlanId)
            .chargePlanMemberId(currentMember.getId().orElseThrow())
            .cycleIndex(currentCycleIndex)
            .amountDue(dueAmount)
            .dueDate(Instant.now().plus(chargePlan.getDueToleranceDays(), ChronoUnit.DAYS))
            .build();
        invoicePersistenceGateway.save(invoice);
    }

    private void processSplit(final ChargePlan chargePlan) {
    }
}
