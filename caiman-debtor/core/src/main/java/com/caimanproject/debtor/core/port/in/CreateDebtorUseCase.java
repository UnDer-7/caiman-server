package com.caimanproject.debtor.core.port.in;

import com.caimanproject.debtor.core.domain.model.Debtor;
import com.caimanproject.debtor.core.port.in.command.CreateDebtorCommand;

public interface CreateDebtorUseCase {

    Debtor execute(CreateDebtorCommand debtor);
}
