package com.caimanproject.billing.core.domain.service;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.model.Invoice;
import com.caimanproject.billing.core.port.out.ChargePlanPersistenceGateway;
import com.caimanproject.billing.core.port.out.ChargePlanSearchGateway;
import com.caimanproject.billing.core.port.out.InvoicePersistenceGateway;
import com.caimanproject.billing.core.port.out.InvoiceSearchGateway;
import com.caimanproject.billing.core.domain.types.InvoiceStatus;
import com.caimanproject.billing.core.test.builder.ChargePlanDomainBuilder;
import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.test.annotation.UnitTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
class RunOdinServiceTest {

    @Mock
    ChargePlanSearchGateway chargePlanSearchGateway;

    @Mock
    InvoiceSearchGateway invoiceSearchGateway;

    @Mock
    InvoicePersistenceGateway invoicePersistenceGateway;

    @Mock
    ChargePlanPersistenceGateway chargePlanPersistenceGateway;

    @InjectMocks
    RunOdinService service;

    @Nested
    @DisplayName("Tests for execute - ROTATING plans")
    class RotatingTestSuit {

        @Test
        void should_not_save_invoice_when_already_generated_today() {
            // Given
            final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .build();
            Mockito.when(chargePlanSearchGateway.getAllActives()).thenReturn(List.of(chargePlan));
            Mockito.when(invoiceSearchGateway.existsGeneratedOn(
                            chargePlan.getId().orElseThrow(), LocalDate.now(ZoneOffset.UTC)))
                    .thenReturn(true);

            // When
            service.execute();

            // Then
            Mockito.verify(invoicePersistenceGateway, Mockito.never()).save(Mockito.any());
            Mockito.verify(invoiceSearchGateway, Mockito.never()).findMaxCycleIndex(Mockito.any());
        }

        @Test
        void should_not_save_invoice_when_no_active_members() {
            // Given
            final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .members(List.of())
                    .build();
            Mockito.when(chargePlanSearchGateway.getAllActives()).thenReturn(List.of(chargePlan));
            Mockito.when(invoiceSearchGateway.existsGeneratedOn(Mockito.any(), Mockito.any()))
                    .thenReturn(false);

            // When
            service.execute();

            // Then
            Mockito.verify(invoicePersistenceGateway, Mockito.never()).save(Mockito.any());
        }

        @Test
        void should_create_invoice_for_member_at_cycle_index_position() {
            // Given
            final var memberRotationOne = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .rotationOrder(1)
                    .build();
            final var memberRotationTwo = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .rotationOrder(2)
                    .build();
            final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .totalAmount(new BigDecimal("90.00"))
                    .members(List.of(memberRotationOne, memberRotationTwo))
                    .build();
            Mockito.when(chargePlanSearchGateway.getAllActives()).thenReturn(List.of(chargePlan));
            Mockito.when(invoiceSearchGateway.existsGeneratedOn(Mockito.any(), Mockito.any()))
                    .thenReturn(false);
            Mockito.when(invoiceSearchGateway.findMaxCycleIndex(chargePlan.getId().orElseThrow()))
                    .thenReturn(Optional.of(3L));
            Mockito.when(invoicePersistenceGateway.save(ArgumentMatchers.any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.execute();

            // Then: cycleIndex = 3 + 1 = 4; position = 4 % 2 = 0 -> memberRotationOne
            final var captor = ArgumentCaptor.forClass(Invoice.class);
            Mockito.verify(invoicePersistenceGateway).save(captor.capture());
            final var savedInvoice = captor.getValue();
            Assertions.assertThat(savedInvoice.getChargePlanMemberId())
                    .isEqualTo(memberRotationOne.getId().orElseThrow());
            Assertions.assertThat(savedInvoice.getCycleIndex()).isEqualTo(4L);
            Assertions.assertThat(savedInvoice.getAmountDue()).isEqualByComparingTo("90.00");
            Assertions.assertThat(savedInvoice.getGenerationDate()).isEqualTo(LocalDate.now(ZoneOffset.UTC));
        }

        @Test
        void should_calculate_due_date_from_generation_date_not_execution_instant() {
            // Given
            final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .dueToleranceDays(7)
                    .build();
            Mockito.when(chargePlanSearchGateway.getAllActives()).thenReturn(List.of(chargePlan));
            Mockito.when(invoiceSearchGateway.existsGeneratedOn(Mockito.any(), Mockito.any()))
                    .thenReturn(false);
            Mockito.when(invoiceSearchGateway.findMaxCycleIndex(Mockito.any())).thenReturn(Optional.empty());
            Mockito.when(invoicePersistenceGateway.save(ArgumentMatchers.any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            final Instant expectedDueDate =
                    LocalDate.now(ZoneOffset.UTC).plusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();

            // When
            service.execute();

            // Then
            final var captor = ArgumentCaptor.forClass(Invoice.class);
            Mockito.verify(invoicePersistenceGateway).save(captor.capture());
            Assertions.assertThat(captor.getValue().getDueDate()).isEqualTo(expectedDueDate);
        }

        @Test
        void should_persist_updated_charge_plan_when_credit_balance_is_consumed() {
            // Given
            final var member = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .rotationOrder(1)
                    .creditBalance(new BigDecimal("30.00"))
                    .build();
            final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .totalAmount(new BigDecimal("90.00"))
                    .members(List.of(member))
                    .build();
            Mockito.when(chargePlanSearchGateway.getAllActives()).thenReturn(List.of(chargePlan));
            Mockito.when(invoiceSearchGateway.existsGeneratedOn(Mockito.any(), Mockito.any()))
                    .thenReturn(false);
            Mockito.when(invoiceSearchGateway.findMaxCycleIndex(Mockito.any())).thenReturn(Optional.empty());
            Mockito.when(invoicePersistenceGateway.save(ArgumentMatchers.any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.execute();

            // Then: amountDue = 90 - 30 = 60, remaining credit = 0 -> still consumed (30 -> 0), must persist
            final var captor = ArgumentCaptor.forClass(ChargePlan.class);
            Mockito.verify(chargePlanPersistenceGateway).save(captor.capture());
            final var savedMember = captor.getValue().getMembers().getFirst();
            Assertions.assertThat(savedMember.getId()).isEqualTo(member.getId());
            Assertions.assertThat(savedMember.getCreditBalance()).isEqualByComparingTo("0.00");
        }

        @Test
        void should_create_invoice_as_paid_when_credit_balance_fully_covers_amount_due() {
            // Given
            final var member = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .rotationOrder(1)
                    .creditBalance(new BigDecimal("90.00"))
                    .build();
            final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .totalAmount(new BigDecimal("90.00"))
                    .members(List.of(member))
                    .build();
            Mockito.when(chargePlanSearchGateway.getAllActives()).thenReturn(List.of(chargePlan));
            Mockito.when(invoiceSearchGateway.existsGeneratedOn(Mockito.any(), Mockito.any()))
                    .thenReturn(false);
            Mockito.when(invoiceSearchGateway.findMaxCycleIndex(Mockito.any())).thenReturn(Optional.empty());
            Mockito.when(invoicePersistenceGateway.save(ArgumentMatchers.any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.execute();

            // Then: credit (90) fully covers total (90) -> amountDue = 0 -> born PAID
            final var captor = ArgumentCaptor.forClass(Invoice.class);
            Mockito.verify(invoicePersistenceGateway).save(captor.capture());
            final var savedInvoice = captor.getValue();
            Assertions.assertThat(savedInvoice.getAmountDue()).isEqualByComparingTo("0.00");
            Assertions.assertThat(savedInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
            Assertions.assertThat(savedInvoice.getPaidAt()).isPresent();
        }

        @Test
        void should_not_persist_charge_plan_when_credit_balance_is_unchanged() {
            // Given
            final var member = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .rotationOrder(1)
                    .creditBalance(BigDecimal.ZERO)
                    .build();
            final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .totalAmount(new BigDecimal("90.00"))
                    .members(List.of(member))
                    .build();
            Mockito.when(chargePlanSearchGateway.getAllActives()).thenReturn(List.of(chargePlan));
            Mockito.when(invoiceSearchGateway.existsGeneratedOn(Mockito.any(), Mockito.any()))
                    .thenReturn(false);
            Mockito.when(invoiceSearchGateway.findMaxCycleIndex(Mockito.any())).thenReturn(Optional.empty());
            Mockito.when(invoicePersistenceGateway.save(ArgumentMatchers.any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.execute();

            // Then: creditBalance stayed 0 -> 0, nothing changed, no need to persist the plan
            Mockito.verify(chargePlanPersistenceGateway, Mockito.never()).save(Mockito.any());
        }

        @Test
        void should_throw_domain_exception_when_charge_plan_has_no_id() {
            // Given
            final var chargePlanWithoutId = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .id(null)
                    .build();
            Mockito.when(chargePlanSearchGateway.getAllActives()).thenReturn(List.of(chargePlanWithoutId));

            // When / Then
            Assertions.assertThatThrownBy(() -> service.execute()).isInstanceOf(DomainException.class);
            Mockito.verify(invoiceSearchGateway, Mockito.never()).existsGeneratedOn(Mockito.any(), Mockito.any());
            Mockito.verify(invoicePersistenceGateway, Mockito.never()).save(Mockito.any());
        }

        @Test
        void should_propagate_domain_exception_when_active_member_has_no_rotation_order() {
            // Given
            final var memberWithoutRotationOrder = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .rotationOrder(null)
                    .build();
            final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .members(List.of(memberWithoutRotationOrder))
                    .build();
            Mockito.when(chargePlanSearchGateway.getAllActives()).thenReturn(List.of(chargePlan));
            Mockito.when(invoiceSearchGateway.existsGeneratedOn(Mockito.any(), Mockito.any()))
                    .thenReturn(false);

            // When / Then
            Assertions.assertThatThrownBy(() -> service.execute()).isInstanceOf(DomainException.class);
            Mockito.verify(invoicePersistenceGateway, Mockito.never()).save(Mockito.any());
        }
    }
}
