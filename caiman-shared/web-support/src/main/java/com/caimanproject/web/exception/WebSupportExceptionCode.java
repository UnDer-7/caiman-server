package com.caimanproject.web.exception;

import com.caimanproject.contracts.exception.CaimanException;
import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WebSupportExceptionCode implements ExceptionCode {

    UNEXPECTED_ERROR("001", "Some unexpected error occurred") {
        @Override
        public CaimanException createException(final String detail) {
            return new UnexpectedException(this, detail);
        }

        @Override
        public CaimanException createException(final String detail, final Throwable originalCause) {
            return new UnexpectedException(this, detail, originalCause);
        }

        @Override
        public CaimanException createException(final Throwable originalCause) {
            return new UnexpectedException(this, originalCause);
        }

        @Override
        public CaimanException createException() {
            return new UnexpectedException(this);
        }
    },
    INVALID_VALUES("002", "Some invalid values were sent") {
        @Override
        public CaimanException createException(final String detail) {
            return new EntrypointInvalidValuesException(this, detail);
        }

        @Override
        public CaimanException createException(final String detail, final Throwable originalCause) {
            return new EntrypointInvalidValuesException(this, detail, originalCause);
        }

        @Override
        public CaimanException createException(final Throwable originalCause) {
            return new EntrypointInvalidValuesException(this, originalCause);
        }

        @Override
        public CaimanException createException() {
            return new EntrypointInvalidValuesException(this);
        }
    };

    private final String code;
    private final String message;

    @Override
    public ModulePrefix getModulePrefix() {
        return ModulePrefix.WEB_SUPPORT;
    }
}
