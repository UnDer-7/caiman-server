package com.caimanproject.contracts.exception;

public abstract class TechnicalException extends CaimanException{

    private static final ErrorHttpStatus HTTP_STATUS = ErrorHttpStatus.INTERNAL_SERVER_ERROR;

    protected TechnicalException(final ExceptionCode exceptionCode, final String detail, final Throwable originalCause) {
        super(exceptionCode, HTTP_STATUS, detail, originalCause);
    }

    protected TechnicalException(final ExceptionCode exceptionCode, final Throwable originalCause) {
        super(exceptionCode, HTTP_STATUS, originalCause);
    }

    protected TechnicalException(final ExceptionCode exceptionCode, final String detail) {
        super(exceptionCode, HTTP_STATUS, detail);
    }

    protected TechnicalException(final ExceptionCode exceptionCode) {
        super(exceptionCode, HTTP_STATUS);
    }

    @Override
    protected final LogLevel getLogLevel() {
        return  LogLevel.ERROR;
    }
}
