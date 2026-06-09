package com.caimanproject.debtor.core.domain.model;

import com.caimanproject.debtor.core.domain.exception.domain.DomainExceptionCode;
import com.caimanproject.debtor.core.domain.types.ContactType;
import com.caimanproject.debtor.core.test.builder.DomainBuilder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DebtorTest {

    @Nested
    @DisplayName("Tests for getDuplicateContactsByValue")
    class getDuplicateContactsByValueTestSuit {

        @ParameterizedTest
        @MethodSource("com.caimanproject.debtor.core.domain.model.DebtorTest#build_duplicate_contacts_by_values")
        void should_return_duplicate_contacts(final List<DebtorContact> contacts, final List<String> expectedDuplicateValues) {
            // When
            final var result = Debtor.getDuplicateContactsByValue(contacts);

            // Then
            Assertions.assertThat(result)
                .extracting(DebtorContact::getContactValue)
                .containsExactlyInAnyOrder(expectedDuplicateValues.toArray(new String[0]));
        }

        @ParameterizedTest
        @MethodSource("should_return_empty_when_no_duplicates__cases")
        void should_return_empty_when_no_duplicates(final List<DebtorContact> contacts) {
            // When
            final var result = Debtor.getDuplicateContactsByValue(contacts);

            // Then
            Assertions.assertThat(result).isEmpty();
        }

        static Stream<Arguments> should_return_empty_when_no_duplicates__cases() {
            return Stream.of(
                Arguments.of(List.of(
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe11@gmail.com").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe22@gmail.com").priority(1).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("marty@gmail.com").priority(2).build())),

                Arguments.of(List.of(
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe11@gmail.com").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.MOBILE_PHONE).contactValue("+0111988883333").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.WHATSAPP).contactValue("+0111988883333").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.TELEGRAM).contactValue("+0111988883333").priority(0).build()
                                    )),

                Arguments.of(List.of(
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.MOBILE_PHONE).contactValue("+0111988883333").priority(0).build())),

                Arguments.of(List.of())
            );
        }
    }

    @Nested
    @DisplayName("Tests for getDuplicateContactsByPriority")
    class getDuplicateContactsByPriorityTestSuit {

        @ParameterizedTest
        @MethodSource("com.caimanproject.debtor.core.domain.model.DebtorTest#build_duplicate_contacts_by_priority")
        void should_return_duplicate_contacts(final List<DebtorContact> contacts, final List<String> expectedDuplicateValues) {
            // When
            final var result = Debtor.getDuplicateContactsByPriority(contacts);

            // Then
            Assertions.assertThat(result)
                .extracting(DebtorContact::getContactValue)
                .containsExactlyInAnyOrder(expectedDuplicateValues.toArray(new String[0]));
        }

        @ParameterizedTest
        @MethodSource("should_return_empty_when_no_duplicates__cases")
        void should_return_empty_when_no_duplicates(final List<DebtorContact> contacts) {
            // When
            final var result = Debtor.getDuplicateContactsByPriority(contacts);

            // Then
            Assertions.assertThat(result).isEmpty();
        }

        static Stream<Arguments> should_return_empty_when_no_duplicates__cases() {
            return Stream.of(
                Arguments.of(List.of(
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe@gmail.com").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("marty@gmail.com").priority(1).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("other@gmail.com").priority(2).build())),

                Arguments.of(List.of(
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe@gmail.com").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.MOBILE_PHONE).contactValue("+5561988880001").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.WHATSAPP).contactValue("+5561988880002").priority(0).build())),

                Arguments.of(List.of(
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe@gmail.com").priority(0).build())),

                Arguments.of(List.of())
            );
        }
    }

    @Nested
    @DisplayName("Tests for Debtor constructor")
    class ConstructorTestSuit {

        @Nested
        @DisplayName("createBuilder")
        class TestCreateBuilder {

            @ParameterizedTest
            @MethodSource("com.caimanproject.debtor.core.domain.model.DebtorTest#build_duplicate_contacts_by_values")
            void should_fail_when_contacts_have_duplicate_value(final List<DebtorContact> contacts, final List<String> repeatedContactValue) {
                // Given
                final var debtorBuilder = Debtor.createBuilder()
                    .name("John Doe")
                    .notes("Lorem ipsum")
                    .notificationsEnabled(true)
                    .contacts(contacts);

                // When
                final var abstractThrowableAssert = Assertions.assertThatThrownBy(debtorBuilder::build);

                // Then
                final var expectedException = DomainExceptionCode.DUPLICATED_CONTACT_VALUE.createException();
                abstractThrowableAssert.isInstanceOf(expectedException.getClass())
                    .hasMessageContainingAll(repeatedContactValue.toArray(new String[0]));
            }

            @ParameterizedTest
            @MethodSource("com.caimanproject.debtor.core.domain.model.DebtorTest#build_duplicate_contacts_by_priority")
            void should_fail_when_contacts_have_duplicate_priority(final List<DebtorContact> contacts, final List<String> repeatedContactValue) {
                // Given
                final var debtorBuilder = Debtor.createBuilder()
                    .name("John Doe")
                    .notes("Lorem ipsum")
                    .notificationsEnabled(true)
                    .contacts(contacts);

                // When
                final var abstractThrowableAssert = Assertions.assertThatThrownBy(debtorBuilder::build);

                // Then
                final var expectedException = DomainExceptionCode.DUPLICATE_CONTACT_PRIORITY.createException();
                abstractThrowableAssert.isInstanceOf(expectedException.getClass())
                    .hasMessageContainingAll(repeatedContactValue.toArray(new String[0]));
            }

            @Test
            void should_build_without_exception() {
                Assertions.assertThatCode(() -> Debtor.createBuilder()
                    .name("John Doe")
                    .notes("Lorem ipsum")
                    .notificationsEnabled(true)
                    .contacts(List.of(
                        DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe@gmail.com").priority(0).build(),
                        DomainBuilder.buildDebtorContactFull().contactType(ContactType.MOBILE_PHONE).contactValue("+5561988883333").priority(1).build()))
                    .build()
                ).doesNotThrowAnyException();
            }
        }

        @Nested
        @DisplayName("restoreBuilder")
        class TestRestoreBuilder {

            @ParameterizedTest
            @MethodSource("com.caimanproject.debtor.core.domain.model.DebtorTest#build_duplicate_contacts_by_values")
            void should_fail_when_contacts_have_duplicate_value(final List<DebtorContact> contacts, final List<String> repeatedContactValue) {
                // Given
                final var debtorBuilder = Debtor.restoreBuilder()
                    .id(UUID.randomUUID())
                    .name("John Doe")
                    .notes("Lorem ipsum")
                    .notificationsEnabled(true)
                    .active(true)
                    .contacts(contacts)
                    .audit(DomainBuilder.buildAuditFull().build());

                // When
                final var abstractThrowableAssert = Assertions.assertThatThrownBy(debtorBuilder::build);

                // Then
                final var expectedException = DomainExceptionCode.DUPLICATED_CONTACT_VALUE.createException();
                abstractThrowableAssert.isInstanceOf(expectedException.getClass())
                    .hasMessageContainingAll(repeatedContactValue.toArray(new String[0]));
            }

            @ParameterizedTest
            @MethodSource("com.caimanproject.debtor.core.domain.model.DebtorTest#build_duplicate_contacts_by_priority")
            void should_fail_when_contacts_have_duplicate_priority(final List<DebtorContact> contacts, final List<String> repeatedContactValue) {
                // Given
                final var debtorBuilder = Debtor.restoreBuilder()
                    .id(UUID.randomUUID())
                    .name("John Doe")
                    .notes("Lorem ipsum")
                    .notificationsEnabled(true)
                    .active(true)
                    .contacts(contacts)
                    .audit(DomainBuilder.buildAuditFull().build());

                // When
                final var abstractThrowableAssert = Assertions.assertThatThrownBy(debtorBuilder::build);

                // Then
                final var expectedException = DomainExceptionCode.DUPLICATE_CONTACT_PRIORITY.createException();
                abstractThrowableAssert.isInstanceOf(expectedException.getClass())
                    .hasMessageContainingAll(repeatedContactValue.toArray(new String[0]));
            }

            @Test
            void should_build_without_exception() {
                Assertions.assertThatCode(() -> Debtor.restoreBuilder()
                    .id(UUID.randomUUID())
                    .name("John Doe")
                    .notes("Lorem ipsum")
                    .notificationsEnabled(true)
                    .active(true)
                    .contacts(List.of(
                        DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe@gmail.com").priority(0).build(),
                        DomainBuilder.buildDebtorContactFull().contactType(ContactType.MOBILE_PHONE).contactValue("+5561988883333").priority(1).build()))
                    .audit(DomainBuilder.buildAuditFull().build())
                    .build()
                ).doesNotThrowAnyException();
            }
        }
    }

    static Stream<Arguments> build_duplicate_contacts_by_values() {
        return Stream.of(
            Arguments.of(
                List.of(
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe11@gmail.com").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe11@gmail.com").priority(1).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("marty@gmail.com").priority(2).build()),
                List.of("johndoe11@gmail.com")),

            Arguments.of(
                List.of(
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe11@gmail.com").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe11@gmail.com").priority(1).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe22@gmail.com").priority(2).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe22@gmail.com").priority(3).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("marty@gmail.com").priority(4).build()),
                List.of("johndoe11@gmail.com", "johndoe22@gmail.com")),

            Arguments.of(
                List.of(
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe11@gmail.com").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe11@gmail.com").priority(1).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe11@gmail.com").priority(2).build()),
                List.of("johndoe11@gmail.com")),

            Arguments.of(
                List.of(
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.MOBILE_PHONE).contactValue("+5561988883333").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.MOBILE_PHONE).contactValue("+5561988883333").priority(1).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.MOBILE_PHONE).contactValue("+0111988883333").priority(2).build()),
                List.of("+5561988883333")),

            Arguments.of(
                List.of(
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("joji@gmail.com").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe11@gmail.com").priority(1).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe11@gmail.com").priority(2).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.MOBILE_PHONE).contactValue("+0111988883333").priority(3).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.MOBILE_PHONE).contactValue("+0111988883333").priority(4).build()),
                List.of("johndoe11@gmail.com", "+0111988883333"))
                        );
    }

    static Stream<Arguments> build_duplicate_contacts_by_priority() {
        return Stream.of(
            Arguments.of(
                List.of(
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe@gmail.com").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("marty@gmail.com").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("other@gmail.com").priority(1).build()),
                List.of("johndoe@gmail.com")),

            Arguments.of(
                List.of(
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe@gmail.com").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("marty@gmail.com").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.MOBILE_PHONE).contactValue("+5561988880001").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.MOBILE_PHONE).contactValue("+5561988880002").priority(0).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("carol@gmail.com").priority(1).build()),
                List.of("johndoe@gmail.com", "+5561988880001")),

            Arguments.of(
                List.of(
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.MOBILE_PHONE).contactValue("+5561988883333").priority(1).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.MOBILE_PHONE).contactValue("+0111988883333").priority(1).build(),
                    DomainBuilder.buildDebtorContactFull().contactType(ContactType.EMAIL).contactValue("johndoe@gmail.com").priority(0).build()),
                List.of("+5561988883333"))
        );
    }
}