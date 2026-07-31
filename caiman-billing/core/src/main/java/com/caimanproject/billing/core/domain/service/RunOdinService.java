package com.caimanproject.billing.core.domain.service;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.model.Invoice;
import com.caimanproject.billing.core.domain.types.DomainExceptionCode;
import com.caimanproject.billing.core.port.in.RunOdinUseCase;
import com.caimanproject.billing.core.port.out.ChargePlanPersistenceGateway;
import com.caimanproject.billing.core.port.out.ChargePlanSearchGateway;
import com.caimanproject.billing.core.port.out.InvoicePersistenceGateway;
import com.caimanproject.billing.core.port.out.InvoiceSearchGateway;
import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.contracts.exception.LogField;
import com.caimanproject.contracts.validation.ValidationError;
import com.caimanproject.contracts.validation.ValidationErrorSourceBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunOdinService implements RunOdinUseCase {

    private final ChargePlanSearchGateway chargePlanSearchGateway;
    private final ChargePlanPersistenceGateway chargePlanPersistenceGateway;
    private final InvoiceSearchGateway invoiceSearchGateway;
    private final InvoicePersistenceGateway invoicePersistenceGateway;

    @Override
    public void execute() {
        final var today = LocalDate.now(ZoneOffset.UTC);
        chargePlanSearchGateway.getAllActives()
            .stream()
            .filter(cp -> cp.isGenerationDueOn(today))
            .forEach(cp -> {
                switch (cp.getType()) {
                    case ROTATING -> processRotating(cp, today);
                    case SPLIT -> processSplit(cp, today);
                };
            });
    }

    private void processRotating(final ChargePlan chargePlan, final LocalDate today) {
        final UUID chargePlanId = requireChargePlanId(chargePlan);

        if (invoiceSearchGateway.existsGeneratedOn(chargePlanId, today)) {
            log.warn(
                    LogField.Placeholders.TWO.getPlaceholder(),
                    StructuredArguments.kv(LogField.MSG.label(), "invoice already generated today, skipping"),
                    StructuredArguments.kv(LogField.CHARGE_PLAN_ID.label(), chargePlanId));
            return;
        }

        final var activeMembers = chargePlan.getActiveMembersOrderedByRotation();

        if (activeMembers.isEmpty()) {
            log.warn(
                    LogField.Placeholders.TWO.getPlaceholder(),
                    StructuredArguments.kv(
                            LogField.MSG.label(), "no active members, skipping ROTATING invoice generation"),
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
                .dueDate(dueDateFor(chargePlan, today))
                .build();

        invoicePersistenceGateway.save(invoice);

        if (currentMember.getCreditBalance().compareTo(charge.updatedMember().getCreditBalance()) != 0) {
            chargePlanPersistenceGateway.save(chargePlan.withUpdatedMember(charge.updatedMember()));
        }

        // todo: send notifications
    }

    private void processSplit(final ChargePlan chargePlan, final LocalDate today) {
    }

    private static Instant dueDateFor(final ChargePlan chargePlan, final LocalDate today) {
        return today.plusDays(chargePlan.getDueToleranceDays()).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static UUID requireChargePlanId(final ChargePlan chargePlan) {
        return chargePlan.getId().orElseThrow(() -> new DomainException(List.of(ValidationError.builder()
                .code(DomainExceptionCode.INVALID_VALUE)
                .source(new ValidationErrorSourceBody("$.chargePlan.id", null))
                .detail("ChargePlan returned by search gateway without a persisted id")
                .build())));
    }
}
