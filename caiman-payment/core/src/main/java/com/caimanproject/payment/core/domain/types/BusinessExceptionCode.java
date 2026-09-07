package com.caimanproject.payment.core.domain.types;

import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessExceptionCode implements ExceptionCode {
    INVOICE_NOT_FOUND("001", "No invoice was found for the given upload token"),
    ACTIVE_PROOF_EXISTS("002", "A payment proof is already pending review for this invoice"),
    INVOICE_NOT_PAYABLE("003", "This invoice no longer accepts a payment proof");

    private final String code;
    private final String message;

    @Override
    public ModulePrefix getModulePrefix() {
        return ModulePrefix.PAYMENT;
    }
}
