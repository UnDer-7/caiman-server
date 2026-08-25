package com.caimanproject.billing.core.domain.service.odin;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.model.ChargePlanMember;
import com.caimanproject.billing.core.domain.model.Invoice;
import com.caimanproject.billing.core.port.out.ChargePlanPersistenceGateway;
import com.caimanproject.billing.core.port.out.InvoicePersistenceGateway;
import com.caimanproject.billing.core.port.out.InvoiceSearchGateway;
import com.caimanproject.billing.core.port.out.NotifyInvoiceCreationGateway;
import com.caimanproject.contracts.exception.LogField;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class SplitInvoiceGenerator {

    private static final int DEFAULT_MAX_ATTEMPTS = 5; // BUSINESS_RULES.md §3.5 default

    private final InvoiceSearchGateway invoiceSearchGateway;
    private final InvoicePersistenceGateway invoicePersistenceGateway;
    private final ChargePlanPersistenceGateway chargePlanPersistenceGateway;
    private final NotifyInvoiceCreationGateway notifyInvoiceCreationGateway;

    void generate(final ChargePlan chargePlan, final LocalDate today) {
        final var chargePlanId = chargePlan.requireId();

        if (invoiceSearchGateway.existsAny(chargePlanId)) {
            log.warn(
                    LogField.Placeholders.THREE.getPlaceholder(),
                    StructuredArguments.kv(
                            LogField.MSG.label(), "SPLIT plan already generated its one-time invoice batch, skipping"),
                    StructuredArguments.kv(LogField.CHARGE_PLAN_NAME.label(), chargePlan.getName()),
                    StructuredArguments.kv(LogField.CHARGE_PLAN_ID.label(), chargePlanId));
            return;
        }

        final var activeMembers = chargePlan.getActiveMembers();
        if (activeMembers.isEmpty()) {
            log.warn(
                    LogField.Placeholders.THREE.getPlaceholder(),
                    StructuredArguments.kv(
                            LogField.MSG.label(), "no active members, skipping SPLIT invoice generation"),
                    StructuredArguments.kv(LogField.CHARGE_PLAN_NAME.label(), chargePlan.getName()),
                    StructuredArguments.kv(LogField.CHARGE_PLAN_ID.label(), chargePlanId));
            return;
        }

        final var baseAmounts = computeNonOverriddenMemberShares(chargePlan.getTotalAmount(), activeMembers);

        var updatedChargePlan = chargePlan;
        for (final ChargePlanMember member : activeMembers) {
            final var memberId = member.getId().orElseThrow();
            final var charge = member.chargeForCycle(baseAmounts.getOrDefault(memberId, BigDecimal.ZERO));

            final Invoice invoice = Invoice.createBuilder()
                    .chargePlanId(chargePlanId)
                    .chargePlanMemberId(memberId)
                    .cycleIndex(0L)
                    .generationDate(today)
                    .amountDue(charge.amountDue())
                    .dueDate(chargePlan.dueDateFrom(today))
                    .build();

            final Invoice saved = invoicePersistenceGateway.save(invoice);

            if (member.getCreditBalance().compareTo(charge.updatedMember().getCreditBalance()) != 0) {
                updatedChargePlan = updatedChargePlan.withUpdatedMember(charge.updatedMember());
            }

            if (chargePlan.getNotificationsEnabled()) {
                chargePlan
                        .getInvoiceCreatedNotification()
                        .ifPresentOrElse(
                                config -> {
                                    final Instant scheduledFor = today.atTime(chargePlan.getNotificationTime())
                                            .atZone(chargePlan.getNotificationTimezone())
                                            .toInstant();
                                    final int maxAttempts =
                                            config.getMaxAttempts().orElse(DEFAULT_MAX_ATTEMPTS);

                                    notifyInvoiceCreationGateway.notify(
                                            saved, chargePlan.getName(), scheduledFor, maxAttempts);
                                },
                                () -> log.warn(
                                        LogField.Placeholders.THREE.getPlaceholder(),
                                        StructuredArguments.kv(
                                                LogField.MSG.label(),
                                                "INVOICE_CREATED notification config not found, skipping notification"),
                                        StructuredArguments.kv(LogField.CHARGE_PLAN_NAME.label(), chargePlan.getName()),
                                        StructuredArguments.kv(LogField.CHARGE_PLAN_ID.label(), chargePlanId)));
            } else {
                log.warn(
                        LogField.Placeholders.THREE.getPlaceholder(),
                        StructuredArguments.kv(
                                LogField.MSG.label(), "notifications disabled for charge plan, skipping notification"),
                        StructuredArguments.kv(LogField.CHARGE_PLAN_NAME.label(), chargePlan.getName()),
                        StructuredArguments.kv(LogField.CHARGE_PLAN_ID.label(), chargePlanId));
            }
        }

        if (updatedChargePlan != chargePlan) {
            chargePlanPersistenceGateway.save(updatedChargePlan);
        }
    }

    /**
     * Computes the amount owed by each member without an {@code amountOverride}, splitting what's left of
     * {@code totalAmount} after subtracting the overridden members' amounts evenly between them. Any leftover cent from
     * rounding is added to the first member in the list.
     */
    private static Map<UUID, BigDecimal> computeNonOverriddenMemberShares(
            final BigDecimal totalAmount, final List<ChargePlanMember> activeMembers) {
        final var plainMembers = activeMembers.stream()
                .filter(member -> member.getAmountOverride().isEmpty())
                .toList();

        final var overriddenSum = activeMembers.stream()
                .map(ChargePlanMember::getAmountOverride)
                .flatMap(Optional::stream)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        final var pool = totalAmount.subtract(overriddenSum).max(BigDecimal.ZERO);

        final Map<UUID, BigDecimal> bases = new HashMap<>();
        if (plainMembers.isEmpty()) {
            return bases;
        }

        final var shareBase = pool.divide(BigDecimal.valueOf(plainMembers.size()), 2, RoundingMode.HALF_UP);
        final var remainder = pool.subtract(shareBase.multiply(BigDecimal.valueOf(plainMembers.size())));

        for (int i = 0; i < plainMembers.size(); i++) {
            final var member = plainMembers.get(i);
            final var base = i == 0 ? shareBase.add(remainder) : shareBase;
            bases.put(member.getId().orElseThrow(), base);
        }

        return bases;
    }
}
