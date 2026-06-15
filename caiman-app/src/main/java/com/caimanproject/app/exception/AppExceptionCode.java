package com.caimanproject.app.exception;

import com.caimanproject.contracts.exception.CaimanException;
import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AppExceptionCode implements ExceptionCode {
    SQLITE_FILE_INITIALIZATION_FAILED("001", "Failed to initialize SQLite database file") {
        @Override
        public CaimanException createException(final String detail) {
            return new SqliteFileInitializationException(this, detail, null);
        }

        @Override
        public CaimanException createException(final String detail, final Throwable originalCause) {
            return new SqliteFileInitializationException(this, detail, originalCause);
        }

        @Override
        public CaimanException createException(final Throwable originalCause) {
            return new SqliteFileInitializationException(this, null, originalCause);
        }

        @Override
        public CaimanException createException() {
            return new SqliteFileInitializationException(this, null, null);
        }
    };

    private final String code;
    private final String message;

    @Override
    public ModulePrefix getModulePrefix() {
        return ModulePrefix.APP;
    }
}
