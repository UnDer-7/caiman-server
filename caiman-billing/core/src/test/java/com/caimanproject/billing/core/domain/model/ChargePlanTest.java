package com.caimanproject.billing.core.domain.model;

import com.caimanproject.billing.core.domain.types.ChargePlanMemberStatus;
import com.caimanproject.billing.core.test.builder.ChargePlanDomainBuilder;
import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.contracts.validation.ValidationErrorSourceBody;
import com.caimanproject.test.annotation.UnitTest;
import java.math.BigDecimal;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class ChargePlanTest {

    @Nested
    @DisplayName("Tests for getActiveMembersOrderedByRotation")
    class GetActiveMembersOrderedByRotationTestSuit {

        @Test
        void should_return_active_members_sorted_by_rotation_order_ascending() {
            // Given
            final var memberRotationTwo = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .rotationOrder(2)
                    .build();
            final var memberRotationOne = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .rotationOrder(1)
                    .build();
            final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .members(List.of(memberRotationTwo, memberRotationOne))
                    .build();

            // When
            final var result = chargePlan.getActiveMembersOrderedByRotation();

            // Then
            Assertions.assertThat(result).containsExactly(memberRotationOne, memberRotationTwo);
        }

        @Test
        void should_exclude_left_members() {
            // Given
            final var activeMember = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .rotationOrder(1)
                    .build();
            final var leftMember = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .rotationOrder(2)
                    .status(ChargePlanMemberStatus.LEFT)
                    .build();
            final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .members(List.of(activeMember, leftMember))
                    .build();

            // When
            final var result = chargePlan.getActiveMembersOrderedByRotation();

            // Then
            Assertions.assertThat(result).containsExactly(activeMember);
        }

        @Test
        void should_throw_domain_exception_when_active_member_has_no_rotation_order() {
            // Given
            final var memberWithoutRotationOrder = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .rotationOrder(null)
                    .build();
            final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .members(List.of(memberWithoutRotationOrder))
                    .build();

            // When
            final var thrown = Assertions.assertThatThrownBy(chargePlan::getActiveMembersOrderedByRotation);

            // Then
            thrown.isInstanceOfSatisfying(DomainException.class, exception -> Assertions.assertThat(
                            exception.getErrors())
                    .anySatisfy(error -> Assertions.assertThat(error.getSource().orElseThrow())
                            .isInstanceOfSatisfying(
                                    ValidationErrorSourceBody.class,
                                    source -> Assertions.assertThat(source.body())
                                            .isEqualTo("$.members[*].rotationOrder"))));
        }
    }

    @Nested
    @DisplayName("Tests for withUpdatedMember")
    class WithUpdatedMemberTestSuit {

        @Test
        void should_replace_matching_member_keeping_others_untouched() {
            // Given
            final var memberOne = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .rotationOrder(1)
                    .build();
            final var memberTwo = ChargePlanDomainBuilder.buildChargePlanMemberFull()
                    .rotationOrder(2)
                    .build();
            final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .members(List.of(memberOne, memberTwo))
                    .build();
            final var updatedMemberOne = ChargePlanMember.restoreBuilder()
                    .id(memberOne.getId().orElseThrow())
                    .debtorId(memberOne.getDebtorId())
                    .amountOverride(memberOne.getAmountOverride().orElse(null))
                    .rotationOrder(memberOne.getRotationOrder().orElse(null))
                    .status(memberOne.getStatus())
                    .creditBalance(new BigDecimal("15.00"))
                    .joinedAt(memberOne.getJoinedAt())
                    .leftAt(memberOne.getLeftAt().orElse(null))
                    .audit(memberOne.getAudit())
                    .build();

            // When
            final var result = chargePlan.withUpdatedMember(updatedMemberOne);

            // Then
            Assertions.assertThat(result).isNotSameAs(chargePlan);
            Assertions.assertThat(result.getMembers()).containsExactlyInAnyOrder(updatedMemberOne, memberTwo);
        }
    }
}
