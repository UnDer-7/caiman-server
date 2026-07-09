package com.caimanproject.contracts.exception;

import com.caimanproject.contracts.validation.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.util.List;

@Slf4j
public class BusinessException extends CaimanException {

    private static final ErrorHttpStatus HTTP_STATUS = ErrorHttpStatus.UNPROCESSABLE_ENTITY;
    private static final String TITLE = "Business rule violation";
    private static final String DETAIL = "One or more business rules were violated. See errors for details.";

    public BusinessException(final List<ValidationError> errors, final Throwable originalCause) {
        super(HTTP_STATUS, TITLE, DETAIL, errors, originalCause);
    }

    public BusinessException(final List<ValidationError> errors) {
        super(HTTP_STATUS, TITLE, DETAIL, errors);
    }

    @Override
    protected final LogLevel getLogLevel() {
        return LogLevel.WARN;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
