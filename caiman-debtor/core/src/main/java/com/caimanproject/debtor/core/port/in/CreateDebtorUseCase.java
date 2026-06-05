package com.caimanproject.debtor.core.port.in;

import com.caimanproject.debtor.core.domain.model.Debtor;

public interface CreateDebtorUseCase {

    Debtor execute(Debtor debtor);
}
