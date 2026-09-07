package com.caimanproject.debtor.core.domain.types;

import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessExceptionCode implements ExceptionCode {
    DUPLICATE_CONTACT_BY_VALUE("001", "Informed contact list has duplicate contact value"),
    DUPLICATE_CONTACT_BY_PRIORITY("002", "Informed contact list has duplicate contact priority");

    private final String code;
    private final String message;

    @Override
    public ModulePrefix getModulePrefix() {
        return ModulePrefix.DEBTOR_BUSINESS;
    }
}
