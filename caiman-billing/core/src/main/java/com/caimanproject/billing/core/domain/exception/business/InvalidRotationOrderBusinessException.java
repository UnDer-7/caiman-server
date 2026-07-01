package com.caimanproject.billing.core.domain.exception.business;

import com.caimanproject.contracts.exception.BusinessException;
import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
class InvalidRotationOrderBusinessException extends BusinessException {

    InvalidRotationOrderBusinessException(final ExceptionCode exceptionCode,
        final String detail, final Throwable originalCause) {
        super(exceptionCode, detail, originalCause);
    }

    InvalidRotationOrderBusinessException(final ExceptionCode exceptionCode, final Throwable originalCause) {
        super(exceptionCode, originalCause);
    }

    InvalidRotationOrderBusinessException(final ExceptionCode exceptionCode, final String detail) {
        super(exceptionCode, detail);
    }

    InvalidRotationOrderBusinessException(final ExceptionCode exceptionCode) {
        super(exceptionCode);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
