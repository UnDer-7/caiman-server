package com.caimanproject.debtor.core.domain.service;

import com.caimanproject.debtor.core.domain.exception.business.BusinessExceptionCode;
import com.caimanproject.debtor.core.port.out.DebtorPersistenceGateway;
import com.caimanproject.debtor.core.test.builder.CommandBuilder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class CreateDebtorServiceTest {

    @Mock
    DebtorPersistenceGateway debtorPersistenceGateway;

    @InjectMocks
    CreateDebtorService service;

    @Test
    void should_fail_when_passing_duplicate_contact_by_priority() {
        // Given
        final var command = CommandBuilder.buildCreateDebtorCommandFull()
            .contacts(List.of(
                CommandBuilder.buildCreateDebtorContactCommand().build(),
                CommandBuilder.buildCreateDebtorContactCommand().build()
                             ))
            .build();

        // When
        final var abstractThrowableAssert = Assertions.assertThatThrownBy(() ->service.execute(command));

        // Then
        final var expectedException = BusinessExceptionCode.DUPLICATE_CONTACT_BY_PRIORITY.createException();

        abstractThrowableAssert.isInstanceOf(expectedException.getClass());
        Mockito.verify(debtorPersistenceGateway, Mockito.never()).save(Mockito.any());
    }
}