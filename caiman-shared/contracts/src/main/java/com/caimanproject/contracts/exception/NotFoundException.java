package com.caimanproject.contracts.exception;

import com.caimanproject.contracts.validation.ValidationError;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
public class NotFoundException extends CaimanException {

    private static final ErrorHttpStatus HTTP_STATUS = ErrorHttpStatus.NOT_FOUND;
    private static final String TITLE = "Not Found";
    private static final String DETAIL = "The requested resource was not found.";

    public NotFoundException(final List<ValidationError> errors, final Throwable originalCause) {
        super(HTTP_STATUS, TITLE, DETAIL, errors, originalCause);
    }

    public NotFoundException(final List<ValidationError> errors) {
        super(HTTP_STATUS, TITLE, DETAIL, errors);
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
