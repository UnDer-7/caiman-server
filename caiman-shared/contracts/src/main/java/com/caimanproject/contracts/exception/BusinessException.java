package com.caimanproject.contracts.exception;

public abstract class BusinessException extends CaimanException {

    private static final ErrorHttpStatus HTTP_STATUS = ErrorHttpStatus.UNPROCESSABLE_ENTITY;

    protected BusinessException(final ExceptionCode exceptionCode, final String detail, final Throwable originalCause) {
        super(exceptionCode, HTTP_STATUS, detail, originalCause);
    }

    protected BusinessException(final ExceptionCode exceptionCode, final Throwable originalCause) {
        super(exceptionCode, HTTP_STATUS, originalCause);
    }

    protected BusinessException(final ExceptionCode exceptionCode, final String detail) {
        super(exceptionCode, HTTP_STATUS, detail);
    }

    protected BusinessException(final ExceptionCode exceptionCode) {
        super(exceptionCode, HTTP_STATUS);
    }

    @Override
    protected final LogLevel getLogLevel() {
        return LogLevel.WARN;
    }
}
