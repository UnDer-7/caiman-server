package com.caimanproject.debtor.infrastructure.database.adapter;

import com.caimanproject.debtor.core.domain.model.Debtor;
import com.caimanproject.debtor.core.port.out.DebtorPersistenceGateway;
import com.caimanproject.debtor.infrastructure.database.entity.DebtorEntity;
import com.caimanproject.debtor.infrastructure.database.mapper.DebtorContactEntityMapper;
import com.caimanproject.debtor.infrastructure.database.mapper.DebtorEntityMapper;
import com.caimanproject.debtor.infrastructure.database.repository.DebtorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
class DebtorPersistenceAdapter implements DebtorPersistenceGateway {

    private final DebtorRepository debtorRepository;
    private final DebtorEntityMapper debtorEntityMapper;
    private final DebtorContactEntityMapper debtorContactEntityMapper;

    @Override
    public Debtor save(final Debtor debtor) {
        final var entity = debtorEntityMapper.toEntity(debtor);
        final var contacts = debtorContactEntityMapper.toEntity(debtor.getContacts());
        entity.addContacts(contacts);

        final var entitySaved = debtorRepository.save(entity);

        return debtorEntityMapper.toModel(entitySaved);
    }

}
