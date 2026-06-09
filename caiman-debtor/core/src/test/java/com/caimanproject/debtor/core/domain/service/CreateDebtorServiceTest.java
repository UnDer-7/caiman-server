package com.caimanproject.debtor.core.domain.service;

import com.caimanproject.debtor.core.domain.exception.business.BusinessExceptionCode;
import com.caimanproject.debtor.core.domain.model.Debtor;
import com.caimanproject.debtor.core.domain.types.ContactType;
import com.caimanproject.debtor.core.port.in.command.CreateDebtorContactCommand;
import com.caimanproject.debtor.core.port.out.DebtorPersistenceGateway;
import com.caimanproject.debtor.core.test.builder.CommandBuilder;
import com.caimanproject.debtor.core.test.builder.DomainBuilder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CreateDebtorServiceTest {

    @Mock
    DebtorPersistenceGateway debtorPersistenceGateway;

    @InjectMocks
    CreateDebtorService service;

    @Nested
    @DisplayName("Tests for duplicate contact by priority")
    class DuplicateContactByPriorityTestSuit {

        @ParameterizedTest
        @MethodSource("com.caimanproject.debtor.core.domain.service.CreateDebtorServiceTest#build_duplicate_contacts_by_priority_commands")
        void should_fail_when_passing_duplicate_contact_by_priority(
            final List<CreateDebtorContactCommand> contacts,
            final List<String> expectedInMessage
        ) {
            // Given
            final var command = CommandBuilder.buildCreateDebtorCommandFull()
                .contacts(contacts)
                .build();

            // When
            final var abstractThrowableAssert = Assertions.assertThatThrownBy(() -> service.execute(command));

            // Then
            final var expectedException = BusinessExceptionCode.DUPLICATE_CONTACT_BY_PRIORITY.createException();
            abstractThrowableAssert.isInstanceOf(expectedException.getClass())
                .hasMessageContainingAll(expectedInMessage.toArray(new String[0]));
            Mockito.verify(debtorPersistenceGateway, Mockito.never()).save(Mockito.any());
        }
    }

    @Nested
    @DisplayName("Tests for duplicate contact by value")
    class DuplicateContactByValueTestSuit {

        @ParameterizedTest
        @MethodSource("com.caimanproject.debtor.core.domain.service.CreateDebtorServiceTest#build_duplicate_contacts_by_value_commands")
        void should_fail_when_passing_duplicate_contact_by_value(
            final List<CreateDebtorContactCommand> contacts,
            final List<String> expectedInMessage
        ) {
            // Given
            final var command = CommandBuilder.buildCreateDebtorCommandFull()
                .contacts(contacts)
                .build();

            // When
            final var abstractThrowableAssert = Assertions.assertThatThrownBy(() -> service.execute(command));

            // Then
            final var expectedException = BusinessExceptionCode.DUPLICATE_CONTACT_BY_VALUE.createException();
            abstractThrowableAssert.isInstanceOf(expectedException.getClass())
                .hasMessageContainingAll(expectedInMessage.toArray(new String[0]));
            Mockito.verify(debtorPersistenceGateway, Mockito.never()).save(Mockito.any());
        }
    }

    @Nested
    @DisplayName("Tests for execute")
    class ExecuteTestSuit {

        @Test
        void should_save_and_return_debtor_when_valid_command() {
            // Given
            final var command = CommandBuilder.buildCreateDebtorCommandFull().build();
            final var savedDebtor = DomainBuilder.buildDebtorFull().build();
            Mockito.when(debtorPersistenceGateway.save(Mockito.any())).thenReturn(savedDebtor);

            // When
            final var result = service.execute(command);

            // Then
            Assertions.assertThat(result).isSameAs(savedDebtor);
            Mockito.verify(debtorPersistenceGateway, Mockito.times(1)).save(Mockito.any(Debtor.class));
        }
    }

    static Stream<Arguments> build_duplicate_contacts_by_priority_commands() {
        return Stream.of(
            Arguments.of(
                List.of(
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.EMAIL).contactValue("a@gmail.com").priority(0).build(),
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.EMAIL).contactValue("b@gmail.com").priority(0).build()),
                List.of("a@gmail.com")),

            Arguments.of(
                List.of(
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.EMAIL).contactValue("a@gmail.com").priority(0).build(),
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.EMAIL).contactValue("b@gmail.com").priority(0).build(),
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.EMAIL).contactValue("c@gmail.com").priority(0).build()),
                List.of("a@gmail.com")),

            Arguments.of(
                List.of(
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.MOBILE_PHONE).contactValue("+5561988880001").priority(0).build(),
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.MOBILE_PHONE).contactValue("+5561988880002").priority(0).build()),
                List.of("+5561988880001"))
        );
    }

    static Stream<Arguments> build_duplicate_contacts_by_value_commands() {
        return Stream.of(
            Arguments.of(
                List.of(
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.EMAIL).contactValue("johndoe@gmail.com").priority(0).build(),
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.EMAIL).contactValue("johndoe@gmail.com").priority(1).build()),
                List.of("johndoe@gmail.com")),

            Arguments.of(
                List.of(
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.MOBILE_PHONE).contactValue("+5561988883333").priority(0).build(),
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.MOBILE_PHONE).contactValue("+5561988883333").priority(1).build(),
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.EMAIL).contactValue("other@gmail.com").priority(2).build()),
                List.of("+5561988883333")),

            Arguments.of(
                List.of(
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.EMAIL).contactValue("johndoe@gmail.com").priority(0).build(),
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.EMAIL).contactValue("johndoe@gmail.com").priority(1).build(),
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.MOBILE_PHONE).contactValue("+0111988883333").priority(2).build(),
                    CommandBuilder.buildCreateDebtorContactCommand().contactType(ContactType.MOBILE_PHONE).contactValue("+0111988883333").priority(3).build()),
                List.of("johndoe@gmail.com", "+0111988883333"))
        );
    }
}
