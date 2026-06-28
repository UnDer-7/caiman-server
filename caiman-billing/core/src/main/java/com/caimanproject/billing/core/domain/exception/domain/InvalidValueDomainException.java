package com.caimanproject.billing.core.domain.exception.domain;

import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
class InvalidValueDomainException extends DomainException {

    InvalidValueDomainException(final ExceptionCode exceptionCode, final String detail, final Throwable originalCause) {
        super(exceptionCode, detail, originalCause);
    }

    InvalidValueDomainException(final ExceptionCode exceptionCode, final Throwable originalCause) {
        super(exceptionCode, originalCause);
    }

    InvalidValueDomainException(final ExceptionCode exceptionCode, final String detail) {
        super(exceptionCode, detail);
    }

    InvalidValueDomainException(final ExceptionCode exceptionCode) {
        super(exceptionCode);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
