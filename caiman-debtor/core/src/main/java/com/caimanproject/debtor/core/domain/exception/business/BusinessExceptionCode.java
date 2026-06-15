package com.caimanproject.debtor.core.domain.exception.business;

import com.caimanproject.contracts.exception.CaimanException;
import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessExceptionCode implements ExceptionCode {
    DUPLICATE_CONTACT_BY_VALUE("001", "Informed contact list has duplicate contact value") {
        @Override
        public CaimanException createException(final String detail) {
            return new DuplicateContactByValueBusinessException(this, detail);
        }

        @Override
        public CaimanException createException(final String detail, final Throwable originalCause) {
            return new DuplicateContactByValueBusinessException(this, detail, originalCause);
        }

        @Override
        public CaimanException createException(final Throwable originalCause) {
            return new DuplicateContactByValueBusinessException(this, originalCause);
        }

        @Override
        public CaimanException createException() {
            return new DuplicateContactByValueBusinessException(this);
        }
    },
    DUPLICATE_CONTACT_BY_PRIORITY("002", "Informed contact list has duplicate contact priority") {
        @Override
        public CaimanException createException(final String detail) {
            return new DuplicateContactByPriorityBusinessException(this, detail);
        }

        @Override
        public CaimanException createException(final String detail, final Throwable originalCause) {
            return new DuplicateContactByPriorityBusinessException(this, detail, originalCause);
        }

        @Override
        public CaimanException createException(final Throwable originalCause) {
            return new DuplicateContactByPriorityBusinessException(this, originalCause);
        }

        @Override
        public CaimanException createException() {
            return new DuplicateContactByPriorityBusinessException(this);
        }
    };

    private final String code;
    private final String message;

    @Override
    public ModulePrefix getModulePrefix() {
        return ModulePrefix.DEBTOR_BUSINESS;
    }
}
