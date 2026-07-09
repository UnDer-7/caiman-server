package com.caimanproject.web.exception;

import com.caimanproject.contracts.exception.CaimanException;
import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WebSupportExceptionCode implements ExceptionCode {
    UNEXPECTED_ERROR("001", "Some unexpected error occurred"),
    INVALID_VALUES("002", "Some invalid values were sent");

    private final String code;
    private final String message;

    @Override
    public ModulePrefix getModulePrefix() {
        return ModulePrefix.WEB_SUPPORT;
    }
}
