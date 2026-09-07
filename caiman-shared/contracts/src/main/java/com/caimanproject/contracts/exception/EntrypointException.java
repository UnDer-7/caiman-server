package com.caimanproject.contracts.exception;

import com.caimanproject.contracts.validation.ValidationError;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
public class EntrypointException extends CaimanException {

    private static final ErrorHttpStatus HTTP_STATUS = ErrorHttpStatus.BAD_REQUEST;
    private static final String TITLE = "Field validation failed";
    private static final String DETAIL = "One or more request fields are invalid. See errors for details.";

    public EntrypointException(final List<ValidationError> errors, final Throwable originalCause) {
        super(HTTP_STATUS, TITLE, DETAIL, errors, originalCause);
    }

    public EntrypointException(final List<ValidationError> errors) {
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
