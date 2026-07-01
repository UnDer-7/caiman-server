package com.caimanproject.billing.core.domain.exception.business;

import com.caimanproject.contracts.exception.BusinessException;
import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
class DebtorNotFoundException extends BusinessException {

    DebtorNotFoundException(final ExceptionCode exceptionCode, final String detail,
        final Throwable originalCause) {
        super(exceptionCode, detail, originalCause);
    }

    DebtorNotFoundException(final ExceptionCode exceptionCode, final Throwable originalCause) {
        super(exceptionCode, originalCause);
    }

    DebtorNotFoundException(final ExceptionCode exceptionCode, final String detail) {
        super(exceptionCode, detail);
    }

    DebtorNotFoundException(final ExceptionCode exceptionCode) {
        super(exceptionCode);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
