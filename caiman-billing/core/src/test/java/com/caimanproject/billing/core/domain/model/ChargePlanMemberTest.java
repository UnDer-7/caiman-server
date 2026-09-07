package com.caimanproject.billing.core.domain.model;

import com.caimanproject.billing.core.test.builder.ChargePlanDomainBuilder;
import com.caimanproject.test.annotation.UnitTest;
import java.math.BigDecimal;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class ChargePlanMemberTest {

    @Nested
    @DisplayName("Tests for chargeForCycle")
    class ChargeForCycleTestSuit {

        @Test
        void should_use_amount_override_when_present() {
            // Given
            final var member = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .amountOverride(new BigDecimal("50.00"))
                    .creditBalance(BigDecimal.ZERO)
                    .build();

            // When
            final var charge = member.chargeForCycle(new BigDecimal("90.00"));

            // Then
            Assertions.assertThat(charge.amountDue()).isEqualByComparingTo("50.00");
            Assertions.assertThat(charge.updatedMember().getCreditBalance()).isEqualByComparingTo("0.00");
        }

        @Test
        void should_fallback_to_plan_total_amount_when_no_override() {
            // Given
            final var member = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .amountOverride(null)
                    .creditBalance(BigDecimal.ZERO)
                    .build();

            // When
            final var charge = member.chargeForCycle(new BigDecimal("90.00"));

            // Then
            Assertions.assertThat(charge.amountDue()).isEqualByComparingTo("90.00");
        }

        @Test
        void should_fallback_to_plan_total_amount_when_override_is_zero() {
            // Given
            final var member = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .amountOverride(BigDecimal.ZERO)
                    .creditBalance(BigDecimal.ZERO)
                    .build();

            // When
            final var charge = member.chargeForCycle(new BigDecimal("90.00"));

            // Then
            Assertions.assertThat(charge.amountDue()).isEqualByComparingTo("90.00");
        }

        @Test
        void should_deduct_credit_balance_from_amount_due_when_partially_covered() {
            // Given
            final var member = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .amountOverride(null)
                    .creditBalance(new BigDecimal("30.00"))
                    .build();

            // When
            final var charge = member.chargeForCycle(new BigDecimal("90.00"));

            // Then
            Assertions.assertThat(charge.amountDue()).isEqualByComparingTo("60.00");
            Assertions.assertThat(charge.updatedMember().getCreditBalance()).isEqualByComparingTo("0.00");
        }

        @Test
        void should_zero_amount_due_and_carry_remaining_credit_when_credit_fully_covers_due() {
            // Given
            final var member = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .amountOverride(null)
                    .creditBalance(new BigDecimal("120.00"))
                    .build();

            // When
            final var charge = member.chargeForCycle(new BigDecimal("90.00"));

            // Then
            Assertions.assertThat(charge.amountDue()).isEqualByComparingTo("0.00");
            Assertions.assertThat(charge.updatedMember().getCreditBalance()).isEqualByComparingTo("30.00");
        }

        @Test
        void should_return_new_member_instance_preserving_other_fields() {
            // Given
            final var member = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .amountOverride(null)
                    .creditBalance(new BigDecimal("10.00"))
                    .build();

            // When
            final var charge = member.chargeForCycle(new BigDecimal("90.00"));

            // Then
            Assertions.assertThat(charge.updatedMember()).isNotSameAs(member);
            Assertions.assertThat(charge.updatedMember().getId()).isEqualTo(member.getId());
            Assertions.assertThat(charge.updatedMember().getDebtorId()).isEqualTo(member.getDebtorId());
            Assertions.assertThat(charge.updatedMember().getRotationOrder()).isEqualTo(member.getRotationOrder());
            Assertions.assertThat(charge.updatedMember().getStatus()).isEqualTo(member.getStatus());
        }
    }
}
