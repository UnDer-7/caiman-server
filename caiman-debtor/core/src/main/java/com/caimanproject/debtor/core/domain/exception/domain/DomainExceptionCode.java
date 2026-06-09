package com.caimanproject.debtor.core.domain.exception.domain;

import com.caimanproject.contracts.exception.CaimanException;
import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DomainExceptionCode implements ExceptionCode {
    INVALID_VALUE(
        "001",
        "Domain received invalid values"
    ) {
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
    },
    DUPLICATED_CONTACT_VALUE("002", "Debtor received duplicated contact value") {
        @Override
        public CaimanException createException(final String detail) {
            return new DuplicateContactValueDomainException(this, detail);
        }

        @Override
        public CaimanException createException(final String detail, final Throwable originalCause) {
            return new DuplicateContactValueDomainException(this, detail, originalCause);
        }

        @Override
        public CaimanException createException(final Throwable originalCause) {
            return new DuplicateContactValueDomainException(this, originalCause);
        }

        @Override
        public CaimanException createException() {
            return new DuplicateContactValueDomainException(this);
        }
    },
    DUPLICATE_CONTACT_PRIORITY("003", "Debtor received duplicate contact priority") {
        @Override
        public CaimanException createException(final String detail) {
            return new DuplicateContactPriorityDomainException(this, detail);
        }

        @Override
        public CaimanException createException(final String detail, final Throwable originalCause) {
            return new DuplicateContactPriorityDomainException(this, detail, originalCause);
        }

        @Override
        public CaimanException createException(final Throwable originalCause) {
            return new DuplicateContactPriorityDomainException(this, originalCause);
        }

        @Override
        public CaimanException createException() {
            return new DuplicateContactPriorityDomainException(this);
        }
    };

    private final String code;
    private final String message;

    @Override
    public ExceptionCode.ModulePrefix getModulePrefix() {
        return ModulePrefix.DEBTOR;
    }

}
