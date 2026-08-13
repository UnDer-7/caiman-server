package com.caimanproject.billing.core.domain.service.odin;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.model.Invoice;
import com.caimanproject.billing.core.port.out.ChargePlanPersistenceGateway;
import com.caimanproject.billing.core.port.out.InvoicePersistenceGateway;
import com.caimanproject.billing.core.port.out.InvoiceSearchGateway;
import com.caimanproject.contracts.exception.LogField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
class RotatingInvoiceGenerator {

    private final InvoiceSearchGateway invoiceSearchGateway;
    private final InvoicePersistenceGateway invoicePersistenceGateway;
    private final ChargePlanPersistenceGateway chargePlanPersistenceGateway;

    void generate(final ChargePlan chargePlan, final LocalDate today) {
        final var chargePlanId = chargePlan.requireId();

        if (invoiceSearchGateway.existsGeneratedOn(chargePlanId, today)) {
            log.warn(
                    LogField.Placeholders.THREE.getPlaceholder(),
                    StructuredArguments.kv(LogField.MSG.label(), "invoice already generated today, skipping"),
                    StructuredArguments.kv(LogField.CHARGE_PLAN_NAME.label(), chargePlan.getName()),
                    StructuredArguments.kv(LogField.CHARGE_PLAN_ID.label(), chargePlanId));
            return;
        }

        final var activeMembers = chargePlan.getActiveMembersOrderedByRotation();
        if (activeMembers.isEmpty()) {
            log.warn(
                    LogField.Placeholders.THREE.getPlaceholder(),
                    StructuredArguments.kv(
                            LogField.MSG.label(), "no active members, skipping ROTATING invoice generation"),
                    StructuredArguments.kv(LogField.CHARGE_PLAN_NAME.label(), chargePlan.getName()),
                    StructuredArguments.kv(LogField.CHARGE_PLAN_ID.label(), chargePlanId));
            return;
        }

        final Long currentCycleIndex =
                invoiceSearchGateway.findMaxCycleIndex(chargePlanId).map(ci -> ci + 1).orElse(0L);
        final var currentMember = activeMembers.get(currentCycleIndex.intValue() % activeMembers.size());
        final var charge = currentMember.chargeForCycle(chargePlan.getTotalAmount());

        final Invoice invoice = Invoice.createBuilder()
                .chargePlanId(chargePlanId)
                .chargePlanMemberId(currentMember.getId().orElseThrow())
                .cycleIndex(currentCycleIndex)
                .generationDate(today)
                .amountDue(charge.amountDue())
                .dueDate(chargePlan.dueDateFrom(today))
                .build();

        invoicePersistenceGateway.save(invoice);

        if (currentMember.getCreditBalance().compareTo(charge.updatedMember().getCreditBalance()) != 0) {
            chargePlanPersistenceGateway.save(chargePlan.withUpdatedMember(charge.updatedMember()));
        }

        // todo: send notifications
    }
}
