package com.caimanproject.web.exception;

import com.caimanproject.contracts.exception.CaimanException;
import com.caimanproject.contracts.exception.ErrorHttpStatus;
import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
public class EntrypointInvalidValuesException extends CaimanException {

    private static final ErrorHttpStatus HTTP_STATUS_CODE = ErrorHttpStatus.BAD_REQUEST;

    EntrypointInvalidValuesException(final ExceptionCode exceptionCode, final String detail, final Throwable originalCause) {
        super(exceptionCode, HTTP_STATUS_CODE, detail, originalCause);
    }

    EntrypointInvalidValuesException(final ExceptionCode exceptionCode, final Throwable originalCause) {
        super(exceptionCode, HTTP_STATUS_CODE, originalCause);
    }

    EntrypointInvalidValuesException(final ExceptionCode exceptionCode, final String detail) {
        super(exceptionCode, HTTP_STATUS_CODE, detail);
    }

    EntrypointInvalidValuesException(final ExceptionCode exceptionCode) {
        super(exceptionCode, HTTP_STATUS_CODE);
    }

    @Override
    protected LogLevel getLogLevel() {
        return LogLevel.WARN;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
