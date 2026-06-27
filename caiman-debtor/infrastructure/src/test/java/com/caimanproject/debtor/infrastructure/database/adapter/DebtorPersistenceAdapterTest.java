package com.caimanproject.debtor.infrastructure.database.adapter;

import com.caimanproject.debtor.core.test.builder.DebtorDomainBuilder;
import com.caimanproject.debtor.infrastructure.database.entity.DebtorEntity;
import com.caimanproject.debtor.infrastructure.database.mapper.DebtorAuditEntityMapper;
import com.caimanproject.debtor.infrastructure.database.mapper.DebtorAuditEntityMapperImpl;
import com.caimanproject.debtor.infrastructure.database.mapper.DebtorContactEntityMapper;
import com.caimanproject.debtor.infrastructure.database.mapper.DebtorContactEntityMapperImpl;
import com.caimanproject.debtor.infrastructure.database.mapper.DebtorEntityMapper;
import com.caimanproject.debtor.infrastructure.database.mapper.DebtorEntityMapperImpl;
import com.caimanproject.mapper.IdMapper;
import com.caimanproject.debtor.infrastructure.database.repository.DebtorRepository;
import com.caimanproject.mapper.IdMapperImpl;
import com.caimanproject.mapper.OptionalMapper;
import com.caimanproject.mapper.OptionalMapperImpl;
import com.caimanproject.test.annotation.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DebtorPersistenceAdapterTest {

    @Mock
    DebtorRepository debtorRepository;

    @Spy
    IdMapper idMapper = new IdMapperImpl();

    @Spy
    OptionalMapper optionalMapper = new OptionalMapperImpl();

    @Spy
    DebtorAuditEntityMapper debtorAuditEntityMapper = new DebtorAuditEntityMapperImpl(optionalMapper);

    @Spy
    DebtorContactEntityMapper debtorContactEntityMapper =
            new DebtorContactEntityMapperImpl(idMapper, debtorAuditEntityMapper);

    @Spy
    DebtorEntityMapper debtorEntityMapper =
            new DebtorEntityMapperImpl(optionalMapper, idMapper, debtorContactEntityMapper, debtorAuditEntityMapper);

    @InjectMocks
    DebtorPersistenceAdapter adapter;

    @Test
    void should_successfully_save_debtor() {
        final var debtor = DebtorDomainBuilder.buildDebtorFull().build();
        when(debtorRepository.save(any(DebtorEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        final var result = adapter.save(debtor);

        verify(debtorRepository).save(any(DebtorEntity.class));
        assertThat(result.getName()).isEqualTo(debtor.getName());
        assertThat(result.getNotes()).isEqualTo(debtor.getNotes());
        assertThat(result.getNotificationsEnabled()).isEqualTo(debtor.getNotificationsEnabled());
        assertThat(result.getActive()).isEqualTo(debtor.getActive());
        assertThat(result.getContacts()).hasSize(debtor.getContacts().size());
    }
}
