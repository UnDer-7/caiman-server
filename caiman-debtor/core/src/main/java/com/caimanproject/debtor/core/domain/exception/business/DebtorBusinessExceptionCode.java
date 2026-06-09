package com.caimanproject.debtor.core.domain.exception.business;

import com.caimanproject.contracts.exception.CaimanException;
import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@RequiredArgsConstructor
public enum DebtorBusinessExceptionCode implements ExceptionCode {
    DUPLICATE_CONTACT("001", "Informed contact list has duplicate contact") {
        @Override
        public CaimanException createException(final String detail) {
            return new DuplicateContactBusinessException(this,detail);
        }

        @Override
        public CaimanException createException(final String detail, final Throwable originalCause) {
            return new DuplicateContactBusinessException(this, detail, originalCause);
        }

        @Override
        public CaimanException createException(final Throwable originalCause) {
            return new DuplicateContactBusinessException(this, originalCause);
        }

        @Override
        public CaimanException createException() {
            return new DuplicateContactBusinessException(this);
        }
    };

    private final String code;
    private final String message;

    @Override
    public ModulePrefix getModulePrefix() {
        return ModulePrefix.DEBTOR;
    }

    @Override
    public abstract CaimanException createException(final String detail);

    @Override
    public abstract CaimanException createException(final String detail, final Throwable originalCause);

    @Override
    public abstract CaimanException createException(final Throwable originalCause);

    @Override
    public abstract CaimanException createException();
}
