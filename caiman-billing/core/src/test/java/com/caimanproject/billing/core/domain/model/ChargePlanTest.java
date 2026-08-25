package com.caimanproject.billing.core.domain.model;

import com.caimanproject.billing.core.domain.types.ChargePlanMemberStatus;
import com.caimanproject.billing.core.test.builder.ChargePlanDomainBuilder;
import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.contracts.validation.ValidationErrorSourceBody;
import com.caimanproject.test.annotation.UnitTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
            thrown.isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> Assertions.assertThat(exception.getErrors())
                            .anySatisfy(error -> Assertions.assertThat(
                                            error.getSource().orElseThrow())
                                    .isInstanceOfSatisfying(
                                            ValidationErrorSourceBody.class,
                                            source -> Assertions.assertThat(source.body())
                                                    .isEqualTo("$.members[*].rotationOrder"))));
        }
    }

    @Nested
    @DisplayName("Tests for isGenerationDueOn")
    class IsGenerationDueOnTestSuit {

        @Test
        void should_not_be_due_when_current_date_is_before_starts_at_even_if_cycle_tick_matches() {
            // Given: anchor tick matches today exactly, but starts_at is 30 days in the future
            final var today = LocalDate.of(2026, 6, 27);
            final var futureStartsAt =
                    today.plusDays(30).atStartOfDay(ZoneOffset.UTC).toInstant();
            final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .cycleAnchorDate(today)
                    .startsAt(futureStartsAt)
                    .build();

            // When / Then
            Assertions.assertThat(chargePlan.isGenerationDueOn(today)).isFalse();
        }

        @Test
        void should_be_due_when_current_date_is_on_or_after_starts_at_and_matches_cycle_tick() {
            // Given: anchor tick matches today, starts_at is 30 days in the past
            final var today = LocalDate.of(2026, 6, 27);
            final var pastStartsAt =
                    today.minusDays(30).atStartOfDay(ZoneOffset.UTC).toInstant();
            final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                    .cycleAnchorDate(today)
                    .startsAt(pastStartsAt)
                    .build();

            // When / Then
            Assertions.assertThat(chargePlan.isGenerationDueOn(today)).isTrue();
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
