package com.caimanproject.contracts.exception;

import com.caimanproject.contracts.validation.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.util.List;

@Slf4j
public class TechnicalException extends CaimanException {

    private static final ErrorHttpStatus HTTP_STATUS = ErrorHttpStatus.INTERNAL_SERVER_ERROR;
    private static final String TITLE = "Internal Server Error";
    private static final String DETAIL = "An unexpected error occurred. Please contact support if the problem persists.";

    public TechnicalException(final List<ValidationError> errors, final Throwable originalCause) {
        super(HTTP_STATUS, TITLE, DETAIL, errors, originalCause);
    }

    public TechnicalException(final List<ValidationError> errors) {
        super(HTTP_STATUS, TITLE, DETAIL, errors);
    }

    @Override
    protected LogLevel getLogLevel() {
        return LogLevel.ERROR;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
