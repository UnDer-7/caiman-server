package com.caimanproject.contracts.exception;

public interface ExceptionCode {

    String getCode();

    String getMessage();

    ModulePrefix getModulePrefix();

    default String getFullCode() {
        return this.getModulePrefix().toString() + "_" + this.getCode();
    }

    enum ModulePrefix {
        APP,
        WEB_SUPPORT,

        NOTIFICATION,
        PAYMENT,

        BILLING_DOMAIN,
        BILLING_BUSINESS,

        DEBTOR_DOMAIN,
        DEBTOR_BUSINESS,
    }
}
