package com.caimanproject.contracts.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
public abstract class CaimanException extends RuntimeException {

    private final Instant timestamp;
    private final int httpStatusCode;
    private final Throwable originalCause;
    private final ExceptionCode exceptionCode;

    @Getter(AccessLevel.NONE)
    private final String detail;

    protected CaimanException(final ExceptionCode exceptionCode, final ErrorHttpStatus httpStatusCode, final String detail, final Throwable originalCause) {

        super(getExceptionMessage(exceptionCode, detail, originalCause), originalCause);

        this.timestamp = Instant.now();
        this.detail = detail;
        this.httpStatusCode = httpStatusCode.getValue();
        this.originalCause = originalCause;
        this.exceptionCode = exceptionCode;
    }

    protected CaimanException(final ExceptionCode exceptionCode, final ErrorHttpStatus httpStatusCode, final Throwable originalCause) {

        super(getExceptionMessage(exceptionCode), originalCause);

        this.timestamp = Instant.now();
        this.detail = null;
        this.httpStatusCode = httpStatusCode.getValue();
        this.originalCause = originalCause;
        this.exceptionCode = exceptionCode;
    }

    protected CaimanException(final ExceptionCode exceptionCode, final ErrorHttpStatus httpStatusCode, final String detail) {

        super(getExceptionMessage(exceptionCode, detail));

        this.timestamp = Instant.now();
        this.detail = detail;
        this.httpStatusCode = httpStatusCode.getValue();
        this.originalCause = null;
        this.exceptionCode = exceptionCode;
    }

    protected CaimanException(final ExceptionCode exceptionCode, final ErrorHttpStatus httpStatusCode) {

        super(getExceptionMessage(exceptionCode));

        this.timestamp = Instant.now();
        this.detail = null;
        this.httpStatusCode = httpStatusCode.getValue();
        this.originalCause = null;
        this.exceptionCode = exceptionCode;
    }

    public void executeLogging() {
        final var className = this.getClass().getSimpleName();
        final var defaultMsg = "An exception has occurred";

        final LogLevel logLevel = getLogLevel();

        switch (logLevel) {
            case TRACE ->
                getLogger()
                    .trace(
                        LogField.Placeholders.THREE.getPlaceholder(),
                        StructuredArguments.kv(LogField.MSG.label(), defaultMsg),
                        StructuredArguments.kv(LogField.EXCEPTION_CLASS.label(), className),
                        StructuredArguments.kv(LogField.EXCEPTION_MESSAGE.label(), super.getMessage()));
            case DEBUG ->
                getLogger()
                    .debug(
                        LogField.Placeholders.THREE.getPlaceholder(),
                        StructuredArguments.kv(LogField.MSG.label(), defaultMsg),
                        StructuredArguments.kv(LogField.EXCEPTION_CLASS.label(), className),
                        StructuredArguments.kv(LogField.EXCEPTION_MESSAGE.label(), super.getMessage()));
            case INFO ->
                getLogger()
                    .info(
                        LogField.Placeholders.THREE.getPlaceholder(),
                        StructuredArguments.kv(LogField.MSG.label(), defaultMsg),
                        StructuredArguments.kv(LogField.EXCEPTION_CLASS.label(), className),
                        StructuredArguments.kv(LogField.EXCEPTION_MESSAGE.label(), super.getMessage()));
            case WARN ->
                getLogger()
                    .warn(
                        LogField.Placeholders.THREE.getPlaceholder(),
                        StructuredArguments.kv(LogField.MSG.label(), defaultMsg),
                        StructuredArguments.kv(LogField.EXCEPTION_CLASS.label(), className),
                        StructuredArguments.kv(LogField.EXCEPTION_MESSAGE.label(), super.getMessage()));
            case ERROR ->
                getLogger()
                    .error(
                        LogField.Placeholders.THREE.getPlaceholder(),
                        StructuredArguments.kv(LogField.MSG.label(), defaultMsg),
                        StructuredArguments.kv(LogField.EXCEPTION_CLASS.label(), className),
                        StructuredArguments.kv(LogField.EXCEPTION_MESSAGE.label(), super.getMessage()));

            default -> {
                log.warn(
                    LogField.Placeholders.TWO.getPlaceholder(),
                    StructuredArguments.kv(LogField.MSG.label(), "Log Level Unknown"),
                    StructuredArguments.kv(LogField.LOG_LEVEL.label(), logLevel));

                throw new IllegalStateException("Unmapped log level: " + logLevel);
            }
        }
    }

    protected abstract LogLevel getLogLevel();

    protected abstract Logger getLogger();

    private static String getExceptionMessage(
        final ExceptionCode exceptionCode, final String detail, final Throwable originalCause) {
        return Optional.ofNullable(detail)
            .filter(Predicate.not(String::isBlank))
            .map(d -> "[code: %s] - [message: %s] - [detail: %s] - [originalCauseMessage: %s]"
                .formatted(exceptionCode.getCode(), exceptionCode.getMessage(), d, originalCause.getMessage()))
            .orElseGet(() -> getExceptionMessage(exceptionCode, originalCause));
    }

    private static String getExceptionMessage(final ExceptionCode exceptionCode, final Throwable throwable) {
        return "[code: %s] - [message: %s] - [originalCauseMessage: %s]"
            .formatted(exceptionCode.getCode(), exceptionCode.getMessage(), throwable.getMessage());
    }

    private static String getExceptionMessage(final ExceptionCode exceptionCode) {
        return "[code: %s] - [message: %s]".formatted(exceptionCode.getCode(), exceptionCode.getMessage());
    }

    private static String getExceptionMessage(final ExceptionCode exceptionCode, final String customMessage) {
        return Optional.ofNullable(customMessage)
            .filter(Predicate.not(String::isBlank))
            .map(cm -> "[code: %s] - [msg: %s] - [customMsg: %s]"
                .formatted(exceptionCode.getCode(), exceptionCode.getMessage(), cm))
            .orElseGet(() -> getExceptionMessage(exceptionCode));
    }

    protected enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}
