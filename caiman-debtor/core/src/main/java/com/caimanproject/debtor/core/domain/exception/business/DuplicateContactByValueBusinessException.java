package com.caimanproject.debtor.core.domain.exception.business;

import com.caimanproject.contracts.exception.BusinessException;
import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
class DuplicateContactByValueBusinessException extends BusinessException {

    DuplicateContactByValueBusinessException(final ExceptionCode exceptionCode, final String detail, final Throwable originalCause) {
        super(exceptionCode, detail, originalCause);
    }

    DuplicateContactByValueBusinessException(final ExceptionCode exceptionCode, final Throwable originalCause) {
        super(exceptionCode, originalCause);
    }

    DuplicateContactByValueBusinessException(final ExceptionCode exceptionCode, final String detail) {
        super(exceptionCode, detail);
    }

    DuplicateContactByValueBusinessException(final ExceptionCode exceptionCode) {
        super(exceptionCode);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
