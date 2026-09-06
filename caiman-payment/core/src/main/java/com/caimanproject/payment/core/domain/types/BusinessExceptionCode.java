package com.caimanproject.payment.core.domain.types;

import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessExceptionCode implements ExceptionCode {
    INVOICE_NOT_FOUND("001", "No invoice was found for the given upload token");

    private final String code;
    private final String message;

    @Override
    public ModulePrefix getModulePrefix() {
        return ModulePrefix.PAYMENT;
    }
}
