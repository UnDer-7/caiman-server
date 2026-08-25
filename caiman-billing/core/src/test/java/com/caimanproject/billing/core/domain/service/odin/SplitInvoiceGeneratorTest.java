package com.caimanproject.billing.core.domain.service.odin;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.model.Invoice;
import com.caimanproject.billing.core.domain.types.InvoiceStatus;
import com.caimanproject.billing.core.port.out.ChargePlanPersistenceGateway;
import com.caimanproject.billing.core.port.out.InvoicePersistenceGateway;
import com.caimanproject.billing.core.port.out.InvoiceSearchGateway;
import com.caimanproject.billing.core.test.builder.ChargePlanDomainBuilder;
import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.test.annotation.UnitTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SplitInvoiceGeneratorTest {

    @Mock
    InvoiceSearchGateway invoiceSearchGateway;

    @Mock
    InvoicePersistenceGateway invoicePersistenceGateway;

    @Mock
    ChargePlanPersistenceGateway chargePlanPersistenceGateway;

    @InjectMocks
    SplitInvoiceGenerator generator;

    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);

    @Test
    void should_not_save_invoice_when_already_generated_before() {
        // Given
        final var chargePlan =
                ChargePlanDomainBuilder.buildSplitChargePlanDueTodayFull().build();
        Mockito.when(invoiceSearchGateway.existsAny(chargePlan.getId().orElseThrow()))
                .thenReturn(true);

        // When
        generator.generate(chargePlan, TODAY);

        // Then
        Mockito.verify(invoicePersistenceGateway, Mockito.never()).save(Mockito.any());
        Mockito.verify(chargePlanPersistenceGateway, Mockito.never()).save(Mockito.any());
    }

    @Test
    void should_not_save_invoice_when_no_active_members() {
        // Given
        final var chargePlan = ChargePlanDomainBuilder.buildSplitChargePlanDueTodayFull()
                .members(List.of())
                .build();
        Mockito.when(invoiceSearchGateway.existsAny(Mockito.any())).thenReturn(false);

        // When
        generator.generate(chargePlan, TODAY);

        // Then
        Mockito.verify(invoicePersistenceGateway, Mockito.never()).save(Mockito.any());
    }

    @Test
    void should_create_one_invoice_per_active_member_with_equal_split() {
        // Given
        final var members = List.of(
                ChargePlanDomainBuilder.buildSplitChargePlanMemberFull().build(),
                ChargePlanDomainBuilder.buildSplitChargePlanMemberFull().build(),
                ChargePlanDomainBuilder.buildSplitChargePlanMemberFull().build(),
                ChargePlanDomainBuilder.buildSplitChargePlanMemberFull().build());
        final var chargePlan = ChargePlanDomainBuilder.buildSplitChargePlanDueTodayFull()
                .totalAmount(new BigDecimal("100.00"))
                .members(members)
                .build();
        Mockito.when(invoiceSearchGateway.existsAny(Mockito.any())).thenReturn(false);
        Mockito.when(invoicePersistenceGateway.save(ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        generator.generate(chargePlan, TODAY);

        // Then: 100 / 4 = 25.00 each, cycleIndex always 0
        final var captor = ArgumentCaptor.forClass(Invoice.class);
        Mockito.verify(invoicePersistenceGateway, Mockito.times(4)).save(captor.capture());
        Assertions.assertThat(captor.getAllValues()).allSatisfy(invoice -> {
            Assertions.assertThat(invoice.getAmountDue()).isEqualByComparingTo("25.00");
            Assertions.assertThat(invoice.getCycleIndex()).isEqualTo(0L);
            Assertions.assertThat(invoice.getGenerationDate()).isEqualTo(TODAY);
        });
    }

    @Test
    void should_apply_rounding_remainder_to_first_member() {
        // Given
        final var first =
                ChargePlanDomainBuilder.buildSplitChargePlanMemberFull().build();
        final var second =
                ChargePlanDomainBuilder.buildSplitChargePlanMemberFull().build();
        final var third =
                ChargePlanDomainBuilder.buildSplitChargePlanMemberFull().build();
        final var chargePlan = ChargePlanDomainBuilder.buildSplitChargePlanDueTodayFull()
                .totalAmount(new BigDecimal("10.00"))
                .members(List.of(first, second, third))
                .build();
        Mockito.when(invoiceSearchGateway.existsAny(Mockito.any())).thenReturn(false);
        Mockito.when(invoicePersistenceGateway.save(ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        generator.generate(chargePlan, TODAY);

        // Then: 10.00 / 3 = 3.33 each, 0.01 remainder -> first member gets 3.34
        final var captor = ArgumentCaptor.forClass(Invoice.class);
        Mockito.verify(invoicePersistenceGateway, Mockito.times(3)).save(captor.capture());
        final var amountsByMember = captor.getAllValues().stream()
                .collect(Collectors.toMap(Invoice::getChargePlanMemberId, Invoice::getAmountDue));

        Assertions.assertThat(amountsByMember.get(first.getId().orElseThrow())).isEqualByComparingTo("3.34");
        Assertions.assertThat(amountsByMember.get(second.getId().orElseThrow())).isEqualByComparingTo("3.33");
        Assertions.assertThat(amountsByMember.get(third.getId().orElseThrow())).isEqualByComparingTo("3.33");
    }

    @Test
    void should_use_override_and_split_remaining_pool_among_non_overridden_members() {
        // Given: total 100, Mateus override 30, Rodrigo override 50, Gustavo/Cesar no override -> 10 each
        final var mateus = ChargePlanDomainBuilder.buildSplitChargePlanMemberFull()
                .amountOverride(new BigDecimal("30.00"))
                .build();
        final var rodrigo = ChargePlanDomainBuilder.buildSplitChargePlanMemberFull()
                .amountOverride(new BigDecimal("50.00"))
                .build();
        final var gustavo =
                ChargePlanDomainBuilder.buildSplitChargePlanMemberFull().build();
        final var cesar =
                ChargePlanDomainBuilder.buildSplitChargePlanMemberFull().build();
        final var chargePlan = ChargePlanDomainBuilder.buildSplitChargePlanDueTodayFull()
                .totalAmount(new BigDecimal("100.00"))
                .members(List.of(mateus, rodrigo, gustavo, cesar))
                .build();
        Mockito.when(invoiceSearchGateway.existsAny(Mockito.any())).thenReturn(false);
        Mockito.when(invoicePersistenceGateway.save(ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        generator.generate(chargePlan, TODAY);

        // Then
        final var captor = ArgumentCaptor.forClass(Invoice.class);
        Mockito.verify(invoicePersistenceGateway, Mockito.times(4)).save(captor.capture());
        final Map<UUID, BigDecimal> amountsByMember = captor.getAllValues().stream()
                .collect(Collectors.toMap(Invoice::getChargePlanMemberId, Invoice::getAmountDue));

        Assertions.assertThat(amountsByMember.get(mateus.getId().orElseThrow())).isEqualByComparingTo("30.00");
        Assertions.assertThat(amountsByMember.get(rodrigo.getId().orElseThrow()))
                .isEqualByComparingTo("50.00");
        Assertions.assertThat(amountsByMember.get(gustavo.getId().orElseThrow()))
                .isEqualByComparingTo("10.00");
        Assertions.assertThat(amountsByMember.get(cesar.getId().orElseThrow())).isEqualByComparingTo("10.00");
    }

    @Test
    void should_apply_credit_balance_deduction_per_member() {
        // Given
        final var member = ChargePlanDomainBuilder.buildSplitChargePlanMemberFull()
                .creditBalance(new BigDecimal("10.00"))
                .build();
        final var chargePlan = ChargePlanDomainBuilder.buildSplitChargePlanDueTodayFull()
                .totalAmount(new BigDecimal("100.00"))
                .members(List.of(member))
                .build();
        Mockito.when(invoiceSearchGateway.existsAny(Mockito.any())).thenReturn(false);
        Mockito.when(invoicePersistenceGateway.save(ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        generator.generate(chargePlan, TODAY);

        // Then: sole plain member gets the full 100.00 share, minus 10.00 credit = 90.00
        final var captor = ArgumentCaptor.forClass(Invoice.class);
        Mockito.verify(invoicePersistenceGateway).save(captor.capture());
        Assertions.assertThat(captor.getValue().getAmountDue()).isEqualByComparingTo("90.00");

        final var chargePlanCaptor = ArgumentCaptor.forClass(ChargePlan.class);
        Mockito.verify(chargePlanPersistenceGateway).save(chargePlanCaptor.capture());
        Assertions.assertThat(
                        chargePlanCaptor.getValue().getMembers().getFirst().getCreditBalance())
                .isEqualByComparingTo("0.00");
    }

    @Test
    void should_mark_invoice_as_paid_when_credit_fully_covers_amount_due() {
        // Given
        final var member = ChargePlanDomainBuilder.buildSplitChargePlanMemberFull()
                .creditBalance(new BigDecimal("100.00"))
                .build();
        final var chargePlan = ChargePlanDomainBuilder.buildSplitChargePlanDueTodayFull()
                .totalAmount(new BigDecimal("100.00"))
                .members(List.of(member))
                .build();
        Mockito.when(invoiceSearchGateway.existsAny(Mockito.any())).thenReturn(false);
        Mockito.when(invoicePersistenceGateway.save(ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        generator.generate(chargePlan, TODAY);

        // Then
        final var captor = ArgumentCaptor.forClass(Invoice.class);
        Mockito.verify(invoicePersistenceGateway).save(captor.capture());
        Assertions.assertThat(captor.getValue().getAmountDue()).isEqualByComparingTo("0.00");
        Assertions.assertThat(captor.getValue().getStatus()).isEqualTo(InvoiceStatus.PAID);
        Assertions.assertThat(captor.getValue().getPaidAt()).isPresent();
    }

    @Test
    void should_persist_charge_plan_only_once_even_when_multiple_members_change() {
        // Given: two members with credit balance to consume
        final var memberOne = ChargePlanDomainBuilder.buildSplitChargePlanMemberFull()
                .creditBalance(new BigDecimal("5.00"))
                .build();
        final var memberTwo = ChargePlanDomainBuilder.buildSplitChargePlanMemberFull()
                .creditBalance(new BigDecimal("5.00"))
                .build();
        final var chargePlan = ChargePlanDomainBuilder.buildSplitChargePlanDueTodayFull()
                .totalAmount(new BigDecimal("100.00"))
                .members(List.of(memberOne, memberTwo))
                .build();
        Mockito.when(invoiceSearchGateway.existsAny(Mockito.any())).thenReturn(false);
        Mockito.when(invoicePersistenceGateway.save(ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        generator.generate(chargePlan, TODAY);

        // Then
        Mockito.verify(chargePlanPersistenceGateway, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    void should_not_persist_charge_plan_when_no_credit_balance_changed() {
        // Given
        final var member = ChargePlanDomainBuilder.buildSplitChargePlanMemberFull()
                .creditBalance(BigDecimal.ZERO)
                .build();
        final var chargePlan = ChargePlanDomainBuilder.buildSplitChargePlanDueTodayFull()
                .totalAmount(new BigDecimal("100.00"))
                .members(List.of(member))
                .build();
        Mockito.when(invoiceSearchGateway.existsAny(Mockito.any())).thenReturn(false);
        Mockito.when(invoicePersistenceGateway.save(ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        generator.generate(chargePlan, TODAY);

        // Then
        Mockito.verify(chargePlanPersistenceGateway, Mockito.never()).save(Mockito.any());
    }

    @Test
    void should_calculate_due_date_from_generation_date() {
        // Given
        final var chargePlan = ChargePlanDomainBuilder.buildSplitChargePlanDueTodayFull()
                .dueToleranceDays(7)
                .build();
        Mockito.when(invoiceSearchGateway.existsAny(Mockito.any())).thenReturn(false);
        Mockito.when(invoicePersistenceGateway.save(ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final Instant expectedDueDate =
                TODAY.plusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();

        // When
        generator.generate(chargePlan, TODAY);

        // Then
        final var captor = ArgumentCaptor.forClass(Invoice.class);
        Mockito.verify(invoicePersistenceGateway).save(captor.capture());
        Assertions.assertThat(captor.getValue().getDueDate()).isEqualTo(expectedDueDate);
    }

    @Test
    void should_throw_domain_exception_when_charge_plan_has_no_id() {
        // Given
        final var chargePlanWithoutId = ChargePlanDomainBuilder.buildSplitChargePlanDueTodayFull()
                .id(null)
                .build();

        // When / Then
        Assertions.assertThatThrownBy(() -> generator.generate(chargePlanWithoutId, TODAY))
                .isInstanceOf(DomainException.class);
        Mockito.verify(invoiceSearchGateway, Mockito.never()).existsAny(Mockito.any());
        Mockito.verify(invoicePersistenceGateway, Mockito.never()).save(Mockito.any());
    }
}
