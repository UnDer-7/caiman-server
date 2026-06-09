package com.caimanproject.contracts.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public enum LogField {
    MSG,
    EXCEPTION_CLASS,
    EXCEPTION_MESSAGE,
    LOG_LEVEL,
    ERROR_CODE,
    ERROR_TIMESTAMP,
    ERROR_MESSAGE,
    HTTP_STATUS_CODE,
    ERROR_MODULE_PREFIX,
    ERROR_CODE_FULL,
    ERROR_DETAIL_MESSAGE,
    EXCEPTION_CAUSE,
    EXCEPTION_CAUSE_MSG,
    TIMEZONE_ID;

    public String label() {
        return this.name().toLowerCase();
    }

    @Getter
    @RequiredArgsConstructor
    public enum Placeholders {
        ONE(createPlaceholder(1)),
        TWO(createPlaceholder(2)),
        THREE(createPlaceholder(3)),
        FOUR(createPlaceholder(4)),
        FIVE(createPlaceholder(5)),
        SIX(createPlaceholder(6)),
        SEVEN(createPlaceholder(7)),
        EIGHT(createPlaceholder(8)),
        NINE(createPlaceholder(9)),
        TEN(createPlaceholder(10));

        private final String placeholder;

        public static String createPlaceholder(final int total) {
            return "{} ".repeat(total);
        }
    }

}
