package com.caimanproject.debtor.core.domain.service;

import com.caimanproject.debtor.core.domain.model.Debtor;
import com.caimanproject.debtor.core.port.in.CreateDebtorUseCase;
import com.caimanproject.debtor.core.port.out.DebtorPersistenceGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class CreateDebtorService implements CreateDebtorUseCase {

    private final DebtorPersistenceGateway debtorPersistenceGateway;

    @Override
    public Debtor execute(final Debtor debtor) {
        return debtorPersistenceGateway.save(debtor);
    }

}
