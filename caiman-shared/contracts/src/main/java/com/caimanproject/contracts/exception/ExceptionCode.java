package com.caimanproject.contracts.exception;

public interface ExceptionCode {

    String getCode();
    String getMessage();
    ModulePrefix getModulePrefix();

    CaimanException createException(String detail);
    CaimanException createException(String detail, Throwable originalCause);
    CaimanException createException(Throwable originalCause);
    CaimanException createException();

    default String getFullCode() {
        return this.getModulePrefix().toString() + "_" + this.getCode();
    }

    enum ModulePrefix {
        BILLING,
        DEBTOR,
        NOTIFICATION,
        PAYMENT,
        WEB_SUPPORT
    }
}
