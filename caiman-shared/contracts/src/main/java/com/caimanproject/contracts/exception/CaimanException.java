package com.caimanproject.contracts.exception;

import com.caimanproject.contracts.validation.ValidationError;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;

@Slf4j
@Getter
public abstract class CaimanException extends RuntimeException {

    private final Instant timestamp;
    private final int httpStatusCode;
    private final List<ValidationError> errors;
    private final String title;
    private final String detail;

    @Getter(AccessLevel.NONE)
    private final Throwable originalCause;

    protected CaimanException(
            final ErrorHttpStatus httpStatusCode,
            final String title,
            final String detail,
            final List<ValidationError> errors,
            final Throwable originalCause) {

        super(getExceptionMessage(title, detail, errors, originalCause), originalCause);

        this.title = title;
        this.timestamp = Instant.now();
        this.httpStatusCode = httpStatusCode.getValue();
        this.originalCause = originalCause;
        this.errors = Optional.ofNullable(errors).map(List::copyOf).orElseGet(Collections::emptyList);
        this.detail = detail;
    }

    protected CaimanException(
            final ErrorHttpStatus httpStatusCode,
            final String title,
            final String detail,
            final List<ValidationError> errors) {

        super(getExceptionMessage(title, detail, errors, null));

        this.title = title;
        this.timestamp = Instant.now();
        this.httpStatusCode = httpStatusCode.getValue();
        this.originalCause = null;
        this.errors = Optional.ofNullable(errors).map(List::copyOf).orElseGet(Collections::emptyList);
        this.detail = detail;
    }

    public void executeLogging() {
        final var className = this.getClass().getSimpleName();
        final var errorCodes =
                errors.stream().map(e -> e.getCode().getFullCode()).toList();
        final var logLevel = getLogLevel();
        final var placeholder = LogField.Placeholders.SIX.getPlaceholder();

        final var args = new ArrayList<Object>(List.of(
                StructuredArguments.kv(LogField.MSG.label(), "An exception has occurred"),
                StructuredArguments.kv(LogField.EXCEPTION_CLASS.label(), className),
                StructuredArguments.kv(LogField.EXCEPTION_MESSAGE.label(), super.getMessage()),
                StructuredArguments.kv(LogField.ERROR_CODES.label(), errorCodes),
                StructuredArguments.kv(LogField.HTTP_STATUS_CODE.label(), httpStatusCode),
                StructuredArguments.kv(
                        LogField.ERROR_TIMESTAMP.label(), getTimestamp().toString())));

        if (logLevel == LogLevel.ERROR) {
            args.add(this);
        }

        final var argsArray = args.toArray();
        switch (logLevel) {
            case TRACE -> getLogger().trace(placeholder, argsArray);
            case DEBUG -> getLogger().debug(placeholder, argsArray);
            case INFO -> getLogger().info(placeholder, argsArray);
            case WARN -> getLogger().warn(placeholder, argsArray);
            case ERROR -> getLogger().error(placeholder, argsArray);
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

    public Optional<Throwable> getOriginalCause() {
        return Optional.ofNullable(originalCause);
    }

    private static String getExceptionMessage(
            final String title,
            final String detail,
            final List<ValidationError> errors,
            final Throwable originalCause) {

        final var errorCodes = Objects.requireNonNullElseGet(errors, Collections::<ValidationError>emptyList).stream()
                .map(CaimanException::formatError)
                .collect(Collectors.joining(" | "));

        return Optional.ofNullable(originalCause)
                .map(oc -> "[title: %s] [detail: %s] [errors: %s] [originalCauseMessage: %s] [originalCauseClass: %s]"
                        .formatted(
                                title,
                                detail,
                                errorCodes,
                                oc.getMessage(),
                                oc.getClass().getName()))
                .orElseGet(() -> "[title: %s] [detail: %s] [errors: %s]".formatted(title, detail, errorCodes));
    }

    private static String formatError(final ValidationError error) {
        final var parts = new ArrayList<String>();

        parts.add("code: " + error.getCode().getFullCode());
        error.getDetail().ifPresent(d -> parts.add("detail: " + d));
        error.getSource().ifPresent(s -> parts.add("source: " + s));

        return "{ " + String.join(", ", parts) + " }";
    }

    protected enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR;
    }
}
