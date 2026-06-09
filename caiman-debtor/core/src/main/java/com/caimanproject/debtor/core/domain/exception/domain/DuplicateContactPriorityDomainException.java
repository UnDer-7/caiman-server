package com.caimanproject.debtor.core.domain.exception.domain;

import com.caimanproject.contracts.exception.BusinessException;
import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
class DuplicateContactPriorityDomainException extends BusinessException {

    DuplicateContactPriorityDomainException(final ExceptionCode exceptionCode, final String detail, final Throwable originalCause) {
        super(exceptionCode, detail, originalCause);
    }

    DuplicateContactPriorityDomainException(final ExceptionCode exceptionCode, final Throwable originalCause) {
        super(exceptionCode, originalCause);
    }

    DuplicateContactPriorityDomainException(final ExceptionCode exceptionCode, final String detail) {
        super(exceptionCode, detail);
    }

    DuplicateContactPriorityDomainException(final ExceptionCode exceptionCode) {
        super(exceptionCode);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
