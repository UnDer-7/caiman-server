package com.caimanproject.billing.core.domain.service;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.model.ChargePlanMember;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                    LogField.Placeholders.THREE.getPlaceholder(),
                    StructuredArguments.kv(LogField.MSG.label(), "invoice already generated today, skipping"),
                    StructuredArguments.kv(LogField.CHARGE_PLAN_NAME.label(), chargePlan.getName()),
                    StructuredArguments.kv(LogField.CHARGE_PLAN_ID.label(), chargePlanId)
                    );
            return;
        }

        final var activeMembers = chargePlan.getActiveMembersOrderedByRotation();
        if (activeMembers.isEmpty()) {
            log.warn(
                    LogField.Placeholders.THREE.getPlaceholder(),
                    StructuredArguments.kv(LogField.MSG.label(), "no active members, skipping ROTATING invoice generation"),
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
                .dueDate(dueDateFor(chargePlan, today))
                .build();

        invoicePersistenceGateway.save(invoice);

        if (currentMember.getCreditBalance().compareTo(charge.updatedMember().getCreditBalance()) != 0) {
            chargePlanPersistenceGateway.save(chargePlan.withUpdatedMember(charge.updatedMember()));
        }

        // todo: send notifications
    }

    private void processSplit(final ChargePlan chargePlan, final LocalDate today) {
        final UUID chargePlanId = requireChargePlanId(chargePlan);

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
                StructuredArguments.kv(LogField.MSG.label(), "no active members, skipping SPLIT invoice generation"),
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
                    .dueDate(dueDateFor(chargePlan, today))
                    .build();

            invoicePersistenceGateway.save(invoice);

            if (member.getCreditBalance().compareTo(charge.updatedMember().getCreditBalance()) != 0) {
                updatedChargePlan = updatedChargePlan.withUpdatedMember(charge.updatedMember());
            }
        }

        if (updatedChargePlan != chargePlan) {
            chargePlanPersistenceGateway.save(updatedChargePlan);
        }

        // todo: send notifications
    }

    /**
     * Computes the amount owed by each member without an {@code amountOverride}, splitting what's
     * left of {@code totalAmount} after subtracting the overridden members' amounts evenly between them.
     * Any leftover cent from rounding is added to the first member in the list.
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
