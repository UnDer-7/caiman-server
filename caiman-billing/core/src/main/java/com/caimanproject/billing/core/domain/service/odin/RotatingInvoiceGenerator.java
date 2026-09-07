package com.caimanproject.billing.core.domain.service.odin;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.model.ChargePlanNotificationConfig;
import com.caimanproject.billing.core.domain.model.Invoice;
import com.caimanproject.billing.core.port.out.ChargePlanPersistenceGateway;
import com.caimanproject.billing.core.port.out.InvoicePersistenceGateway;
import com.caimanproject.billing.core.port.out.InvoiceSearchGateway;
import com.caimanproject.billing.core.port.out.NotifyInvoiceCreationGateway;
import com.caimanproject.contracts.exception.LogField;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class RotatingInvoiceGenerator {

    private static final int DEFAULT_MAX_ATTEMPTS = 5; // BUSINESS_RULES.md §3.5 default

    private final InvoiceSearchGateway invoiceSearchGateway;
    private final InvoicePersistenceGateway invoicePersistenceGateway;
    private final ChargePlanPersistenceGateway chargePlanPersistenceGateway;
    private final NotifyInvoiceCreationGateway notifyInvoiceCreationGateway;

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

        final Long currentCycleIndex = invoiceSearchGateway
                .findMaxCycleIndex(chargePlanId)
                .map(ci -> ci + 1)
                .orElse(0L);
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

        final Invoice saved = invoicePersistenceGateway.save(invoice);

        if (currentMember.getCreditBalance().compareTo(charge.updatedMember().getCreditBalance()) != 0) {
            chargePlanPersistenceGateway.save(chargePlan.withUpdatedMember(charge.updatedMember()));
        }

        // todo: ver de nao deixar duplicado, talvez criar um command
        final Instant scheduledFor = today.atTime(chargePlan.getNotificationTime())
                .atZone(chargePlan.getNotificationTimezone())
                .toInstant();
        final Optional<ChargePlanNotificationConfig> invoiceCreatedNotification =
                chargePlan.getInvoiceCreatedNotification();
        final int maxAttempts = invoiceCreatedNotification
                .flatMap(ChargePlanNotificationConfig::getMaxAttempts)
                .orElse(DEFAULT_MAX_ATTEMPTS);
        final boolean invoiceCreatedNotificationEnabled = chargePlan.getNotificationsEnabled()
                && invoiceCreatedNotification
                        .map(ChargePlanNotificationConfig::getEnabled)
                        .orElse(true);

        notifyInvoiceCreationGateway.notify(
                saved,
                currentMember.getDebtorId(),
                chargePlan.getName(),
                invoiceCreatedNotificationEnabled,
                scheduledFor,
                maxAttempts);
    }
}
