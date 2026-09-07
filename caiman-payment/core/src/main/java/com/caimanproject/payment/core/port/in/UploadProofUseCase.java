package com.caimanproject.payment.core.port.in;

import com.caimanproject.payment.core.port.in.command.UploadProofCommand;
import com.caimanproject.payment.core.port.in.result.UploadProofResult;

public interface UploadProofUseCase {
    UploadProofResult execute(UploadProofCommand command);
}
