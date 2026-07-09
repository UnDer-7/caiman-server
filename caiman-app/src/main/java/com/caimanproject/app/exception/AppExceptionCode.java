package com.caimanproject.app.exception;

import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AppExceptionCode implements ExceptionCode {
    SQLITE_FILE_INITIALIZATION_FAILED("001", "Failed to initialize SQLite database file");

    private final String code;
    private final String message;

    @Override
    public ModulePrefix getModulePrefix() {
        return ModulePrefix.APP;
    }
}
