package com.caimanproject.billing.core.domain.exception.domain;

import com.caimanproject.contracts.exception.CaimanException;
import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DomainExceptionCode implements ExceptionCode {
    INVALID_VALUE("001", "Domain received invalid values") {
        @Override
        public CaimanException createException(final String detail) {
            return new InvalidValueDomainException(this, detail);
        }

        @Override
        public CaimanException createException(final String detail, final Throwable originalCause) {
            return new InvalidValueDomainException(this, detail, originalCause);
        }

        @Override
        public CaimanException createException(final Throwable originalCause) {
            return new InvalidValueDomainException(this, originalCause);
        }

        @Override
        public CaimanException createException() {
            return new InvalidValueDomainException(this);
        }
    };

    private final String code;
    private final String message;

    @Override
    public ModulePrefix getModulePrefix() {
        return ModulePrefix.BILLING_DOMAIN;
    }
}
