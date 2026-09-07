package com.caimanproject.payment.core.domain.types;

import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DomainExceptionCode implements ExceptionCode {
    INVALID_VALUE("001", "Domain received invalid values");

    private final String code;
    private final String message;

    @Override
    public ModulePrefix getModulePrefix() {
        return ModulePrefix.PAYMENT_DOMAIN;
    }
}
