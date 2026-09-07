package com.caimanproject.payment.core.port.in;

import com.caimanproject.payment.core.domain.model.ProofPageView;
import com.caimanproject.payment.core.port.in.command.GetProofPageCommand;

public interface GetProofPageUseCase {

    ProofPageView execute(GetProofPageCommand command);
}
