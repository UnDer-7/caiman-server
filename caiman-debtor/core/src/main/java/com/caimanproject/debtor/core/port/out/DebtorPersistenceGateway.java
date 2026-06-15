package com.caimanproject.debtor.core.port.out;

import com.caimanproject.debtor.core.domain.model.Debtor;

public interface DebtorPersistenceGateway {

    Debtor save(final Debtor debtor);
}
