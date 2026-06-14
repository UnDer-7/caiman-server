package com.caimanproject.app.exception;

import com.caimanproject.contracts.exception.ExceptionCode;
import com.caimanproject.contracts.exception.TechnicalException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
public class SqliteFileInitializationException extends TechnicalException {

    SqliteFileInitializationException(final ExceptionCode exceptionCode, final String detail, final Throwable cause) {
        super(exceptionCode, detail, cause);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
