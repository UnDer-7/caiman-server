package com.caimanproject.debtor.core.domain.types;

import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DomainExceptionCode implements ExceptionCode {
    INVALID_VALUE("001", "Domain received invalid values"),
    DUPLICATED_CONTACT_VALUE("002", "Debtor received duplicated contact value"),
    DUPLICATE_CONTACT_PRIORITY("003", "Debtor received duplicate contact priority");

    private final String code;
    private final String message;

    @Override
    public ExceptionCode.ModulePrefix getModulePrefix() {
        return ModulePrefix.DEBTOR_DOMAIN;
    }
}
