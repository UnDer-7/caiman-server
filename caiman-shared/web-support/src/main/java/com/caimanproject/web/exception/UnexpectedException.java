package com.caimanproject.web.exception;

import com.caimanproject.contracts.exception.ExceptionCode;
import com.caimanproject.contracts.exception.TechnicalException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
public class UnexpectedException extends TechnicalException {

    UnexpectedException(final ExceptionCode exceptionCode, final String detail, final Throwable originalCause) {
        super(exceptionCode, detail, originalCause);
    }

    UnexpectedException(final ExceptionCode exceptionCode, final Throwable originalCause) {
        super(exceptionCode, originalCause);
    }

    UnexpectedException(final ExceptionCode exceptionCode, final String detail) {
        super(exceptionCode, detail);
    }

    UnexpectedException(final ExceptionCode exceptionCode) {
        super(exceptionCode);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
