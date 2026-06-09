package com.caimanproject.web.config;

import com.caimanproject.contracts.exception.BusinessException;
import com.caimanproject.contracts.exception.CaimanException;
import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.contracts.exception.LogField;
import com.caimanproject.contracts.exception.TechnicalException;
import com.caimanproject.contracts.util.Constants;
import com.caimanproject.web.constant.OpenApiConstants;
import com.caimanproject.web.dto.response.ErrorResponseDto;
import com.caimanproject.web.exception.EntrypointInvalidValuesException;
import com.caimanproject.web.exception.WebSupportExceptionCode;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalRestExceptionHandlerConfig extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(final Exception exception) {
        if (exception.getCause() instanceof CaimanException caimanException) {
            final String warnMsg = """
                An unexpected exception occurred but with a CaimanException cause, \
                delegating to CaimanException handler
                """;

            log.warn(
                LogField.Placeholders.FIVE.getPlaceholder(),
                StructuredArguments.kv(LogField.MSG.label(), warnMsg),
                StructuredArguments.kv(LogField.EXCEPTION_MESSAGE.label(), exception.getMessage()),
                StructuredArguments.kv(
                    LogField.EXCEPTION_CLASS.label(),
                    exception.getClass().getSimpleName()),
                StructuredArguments.kv(LogField.EXCEPTION_CAUSE.label(), exception.getCause()),
                StructuredArguments.kv(
                    LogField.EXCEPTION_CAUSE_MSG.label(),
                    exception.getCause().getMessage()),
                exception);

            return handleCaimanException(caimanException);
        }

        log.warn(
            LogField.Placeholders.TWO.getPlaceholder(),
            StructuredArguments.kv(LogField.MSG.label(), "An unexpected exception occurred"),
            StructuredArguments.kv(LogField.EXCEPTION_MESSAGE.label(), exception.getMessage()),
            exception);

        final var unexpectedException = WebSupportExceptionCode.UNEXPECTED_ERROR.createException(exception);
        return logExceptionAndBuild(unexpectedException);
    }

    @ExceptionHandler(CaimanException.class)
    public ResponseEntity<Object> handleCaimanException(final CaimanException exception) {
        return logExceptionAndBuild(exception);
    }

    // Handle @NotNull, @NotEmpty, ... errors in request parameters
    // e.g.: @RequestHeader, @RequestParam, @PathVariable
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleUnexpectedException(final ConstraintViolationException exception) {
        final String invalidValues = exception.getConstraintViolations().stream()
            .map(violation -> buildInvalidAttributeMessage(
                violation.getPropertyPath().toString().split("\\.")[1],
                violation.getMessage(),
                violation.getInvalidValue()))
            .collect(Collectors.joining(" | "));

        return logExceptionAndBuild(WebSupportExceptionCode.INVALID_VALUES.createException(invalidValues));
    }

    // Handle @NotNull, @NotEmpty, ... errors in request body
    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
        final MethodArgumentNotValidException ex,
        final HttpHeaders headers,
        final HttpStatusCode status,
        final WebRequest request) {

        final String invalidValues = ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> buildInvalidAttributeMessage(
                fieldError.getField(),
                Objects.requireNonNullElse(fieldError.getDefaultMessage(), "validation failed"),
                fieldError.getRejectedValue()))
            .collect(Collectors.joining(" | "));

        return logExceptionAndBuild(WebSupportExceptionCode.INVALID_VALUES.createException(invalidValues));
    }

    // Handle invalid formats in request parameters
    // e.g.: @RequestHeader, @RequestParam, @PathVariable
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleInvalidRequestParameters(MethodArgumentTypeMismatchException exception) {
        if (Objects.equals(exception.getRequiredType(), ZoneId.class)) {
            return buildInvalidZoneIdResponse(exception, exception.getName(), exception.getValue());
        }

        if (Objects.equals(exception.getRequiredType(), Instant.class)) {
            return buildInvalidInstantResponse(exception, exception.getName(), exception.getValue());
        }

        if (Objects.equals(exception.getRequiredType(), LocalDate.class)) {
            return buildInvalidDateResponse(exception, exception.getName(), exception.getValue());
        }

        if (Objects.equals(exception.getRequiredType(), LocalTime.class)) {
            return buildInvalidTimeResponse(exception, exception.getName(), exception.getValue());
        }

        return buildInvalidRequestParameters(
            exception.getName(), exception.getMessage(), exception.getValue(), exception);
    }

    // Handle invalid formats in request body
    @Override
    public ResponseEntity<Object> handleHttpMessageNotReadable(
        final HttpMessageNotReadableException ex,
        final HttpHeaders headers,
        final HttpStatusCode status,
        final WebRequest request) {

        // Check if the cause is an InvalidFormatException (parsing error)
        if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {
            final Class<?> targetType = invalidFormatException.getTargetType();
            final String fieldName = invalidFormatException.getPath().stream()
                .map(JacksonException.Reference::getPropertyName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));

            final Object invalidValue = invalidFormatException.getValue();

            if (Objects.equals(targetType, ZoneId.class)) {
                return buildInvalidZoneIdResponse(ex, fieldName, invalidValue);
            }

            if (Objects.equals(targetType, Instant.class)) {
                return buildInvalidInstantResponse(ex, fieldName, invalidValue);
            }

            if (Objects.equals(targetType, LocalDate.class)) {
                return buildInvalidDateResponse(ex, fieldName, invalidValue);
            }

            if (Objects.equals(targetType, LocalTime.class)) {
                return buildInvalidTimeResponse(ex, fieldName, invalidValue);
            }

            // Handle other format errors generically
            return buildInvalidRequestParameters(
                fieldName, "invalid format for type " + targetType.getSimpleName(), invalidValue, ex);
        }

        // For other HttpMessageNotReadableException cases, use default handling
        final var invalidValuesException = WebSupportExceptionCode.INVALID_VALUES.createException("Failed to read request: " + ex.getMessage(), ex);
        return logExceptionAndBuild(invalidValuesException);
    }

    private ResponseEntity<Object> logExceptionAndBuild(final CaimanException exception) {
        try {
            MDC.put(LogField.ERROR_CODE.label(), exception.getExceptionCode().getCode());
            MDC.put(LogField.ERROR_MODULE_PREFIX.label(), exception.getExceptionCode().getModulePrefix().toString());
            MDC.put(LogField.ERROR_CODE_FULL.label(), exception.getExceptionCode().getFullCode());
            MDC.put(LogField.ERROR_TIMESTAMP.label(), exception.getTimestamp().toString());
            MDC.put(LogField.ERROR_MESSAGE.label(), exception.getExceptionCode().getMessage());
            MDC.put(LogField.HTTP_STATUS_CODE.label(), Integer.toString(exception.getHttpStatusCode()));;
            exception.getDetail()
                .ifPresent(customMsg -> MDC.put(LogField.ERROR_DETAIL_MESSAGE.label(), customMsg));

            exception.executeLogging();

            final ErrorResponseDto errorDto = buildErrorResponse(exception);
            return ResponseEntity.status(exception.getHttpStatusCode()).body(errorDto);
        } finally {
            MDC.clear();
        }
    }

    private ErrorResponseDto buildErrorResponse(final CaimanException exception) {
        return switch (exception) {
            case EntrypointInvalidValuesException invalidValues -> ErrorResponseDto.builder()
                .code(invalidValues.getExceptionCode().getFullCode())
                .timestamp(invalidValues.getTimestamp())
                .message(invalidValues.getExceptionCode().getMessage())
                .detail(invalidValues.getDetail().orElse(null))
                .httpStatusCode(invalidValues.getHttpStatusCode())
                .build();
            case BusinessException business -> ErrorResponseDto.builder()
                .code(business.getExceptionCode().getFullCode())
                .timestamp(business.getTimestamp())
                .message(business.getExceptionCode().getMessage())
                .detail(business.getDetail().orElse(null))
                .httpStatusCode(business.getHttpStatusCode())
                .build();
            case DomainException domain -> ErrorResponseDto.builder()
                .code(domain.getExceptionCode().getFullCode())
                .timestamp(domain.getTimestamp())
                .message("An internal validation failure occurred.")
                .httpStatusCode(domain.getHttpStatusCode())
                .build();
            case TechnicalException technical -> ErrorResponseDto.builder()
                .code(technical.getExceptionCode().getFullCode())
                .timestamp(technical.getTimestamp())
                .message("An internal server error occurred.")
                .httpStatusCode(technical.getHttpStatusCode())
                .build();
            default -> {
                log.error(
                    LogField.Placeholders.TWO.getPlaceholder(),
                    StructuredArguments.kv(LogField.MSG.label(), "Unhandled CaimanException subtype fell through to default"),
                    StructuredArguments.kv(LogField.EXCEPTION_CLASS.label(), exception.getClass().getName())
                );
                yield ErrorResponseDto.builder()
                    .code(exception.getExceptionCode().getFullCode())
                    .timestamp(exception.getTimestamp())
                    .message("An unexpected error occurred.")
                    .httpStatusCode(500)
                    .build();
            }
        };
    }

    private static String buildInvalidAttributeMessage(
        final String attributeName, final String errMotive, final Object attributeValue) {

        if (errMotive.contains(Instant.class.getName())) {
            final String errMotiveInstant = "date-time must be in the following format: %s (example: %s)"
                .formatted(Constants.DATE_TIME_FORMAT, OpenApiConstants.Examples.DATE_TIME);

            return "[ propertyPath: %s - errorMotive: %s - valueProvided: %s ]"
                .formatted(attributeName, errMotiveInstant, attributeValue);
        }

        return "[ propertyPath: %s - errorMotive: %s - valueProvided: %s ]"
            .formatted(attributeName, errMotive, attributeValue);
    }

    private ResponseEntity<Object> buildInvalidDateResponse(
        final Exception ex, final String fieldName, final Object invalidValue) {
        final String description = "date must be in the following format: %s - example: %s"
            .formatted(Constants.DATE_FORMAT, OpenApiConstants.Examples.DATE);

        return buildInvalidRequestParameters(fieldName, "invalid date format", invalidValue, description, ex);
    }

    private ResponseEntity<Object> buildInvalidTimeResponse(
        final Exception ex, final String fieldName, final Object invalidValue) {
        final String description = "time must be in the following format: %s - example: %s"
            .formatted(Constants.TIME_FORMAT, OpenApiConstants.Examples.TIME);

        return buildInvalidRequestParameters(fieldName, "invalid time format", invalidValue, description, ex);
    }

    private ResponseEntity<Object> buildInvalidInstantResponse(
        final Exception ex, final String fieldName, final Object invalidValue) {
        final String description = "date-time must be in the following format: %s - example: %s"
            .formatted(Constants.DATE_TIME_FORMAT, OpenApiConstants.Examples.DATE_TIME);

        return buildInvalidRequestParameters(fieldName, "invalid date-time format", invalidValue, description, ex);
    }

    private ResponseEntity<Object> buildInvalidZoneIdResponse(
        final Exception ex, final String fieldName, final Object invalidValue) {
        return buildInvalidRequestParameters(
            fieldName,
            "invalid timezone id",
            invalidValue,
            "timezone must be a valid tz database identifier (e.g., America/Sao_Paulo, UTC, Europe/London)",
            ex);
    }

    private ResponseEntity<Object> buildInvalidRequestParameters(
        final String parameterName,
        final String motive,
        final Object valueProvided,
        final String description,
        final Exception originalException) {
        var msg = "[ parameter name: %s - errorMotive: %s - valueProvided: %s - description: %s ]"
            .formatted(parameterName, motive, valueProvided, description);

        final var invalidValuesException = WebSupportExceptionCode.INVALID_VALUES.createException(msg, originalException);
        return logExceptionAndBuild(invalidValuesException);
    }

    private ResponseEntity<Object> buildInvalidRequestParameters(
        final String parameterName,
        final String motive,
        final Object valueProvided,
        final Exception originalException) {
        var msg = "[ parameter name: %s - errorMotive: %s - valueProvided: %s ]"
            .formatted(parameterName, motive, valueProvided);

        final var invalidValuesException = WebSupportExceptionCode.INVALID_VALUES.createException(msg, originalException);
        return logExceptionAndBuild(invalidValuesException);
    }

}
