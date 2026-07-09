package com.caimanproject.web.config;

import com.caimanproject.contracts.exception.CaimanException;
import com.caimanproject.contracts.exception.EntrypointException;
import com.caimanproject.contracts.exception.LogField;
import com.caimanproject.contracts.exception.TechnicalException;
import com.caimanproject.contracts.util.Constants;
import com.caimanproject.contracts.validation.ValidationError;
import com.caimanproject.contracts.validation.ValidationErrorSource;
import com.caimanproject.contracts.validation.ValidationErrorSourceBody;
import com.caimanproject.contracts.validation.ValidationErrorSourceGeneric;
import com.caimanproject.contracts.validation.ValidationErrorSourceHeader;
import com.caimanproject.contracts.validation.ValidationErrorSourceParameter;
import com.caimanproject.contracts.validation.ValidationErrorSourcePathParameter;
import com.caimanproject.web.annotation.composition.body.BodyParam;
import com.caimanproject.web.annotation.composition.header.HeaderParam;
import com.caimanproject.web.annotation.composition.path.PathParam;
import com.caimanproject.web.annotation.composition.query.QueryParam;
import com.caimanproject.web.constant.OpenApiConstants;
import com.caimanproject.web.dto.response.ProblemDetailResponseDto;
import com.caimanproject.web.exception.WebSupportExceptionCode;
import com.caimanproject.web.mapper.CaimanExceptionMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Payload;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalRestExceptionHandlerConfig extends ResponseEntityExceptionHandler {

    private final CaimanExceptionMapper caimanExceptionMapper;

    // Last-resort fallback for any exception not caught by a more specific handler below — genuine unexpected
    // crashes (NPE, etc). If the cause is itself a CaimanException (e.g. rethrown by a proxy/AOP layer),
    // delegates to handleCaimanException instead of wrapping it again. Always maps to TechnicalException (500),
    // never leaks the real exception message/stacktrace to the client — that only goes to the log.
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

        final var validation = ValidationError.builder()
                .code(WebSupportExceptionCode.UNEXPECTED_ERROR)
                .build();
        final var unexpectedException = new TechnicalException(Collections.singletonList(validation));
        return logExceptionAndBuild(unexpectedException);
    }

    // Handles any CaimanException (Business/Domain/Technical/Entrypoint) thrown directly by the application —
    // domain invariants, business rule violations, etc. Just logs and builds the ProblemDetail response; the
    // status/title/detail/errors are already fully resolved by the exception itself.
    @ExceptionHandler(CaimanException.class)
    public ResponseEntity<Object> handleCaimanException(final CaimanException exception) {
        return logExceptionAndBuild(exception);
    }

    // Handles Jakarta method-level validation failures (requires @Validated on the controller class + a
    // constraint annotation directly on the queryParam): @PathVariable, @RequestParam, @RequestHeader, or a
    // simple/primitive @RequestBody value. Does NOT handle @Valid on a @RequestBody DTO's nested fields —
    // that's handleMethodArgumentNotValid below.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(final ConstraintViolationException exception) {
        final var validations = exception.getConstraintViolations().stream()
                .map(violation -> {
                    final var pathSegments =
                            violation.getPropertyPath().toString().split("\\.");
                    final var paramName = pathSegments[pathSegments.length - 1];

                    return ValidationError.builder()
                            .code(WebSupportExceptionCode.INVALID_VALUES)
                            .detail(violation.getMessage())
                            .source(resolveSource(violation, paramName))
                            .build();
                })
                .toList();

        return logExceptionAndBuild(new EntrypointException(validations));
    }

    // Handles @Valid on a @RequestBody DTO's nested fields (e.g. a plain @NotBlank on a field inside
    // CreateChargePlanRequestDto). Always originates from the request body — unlike handleConstraintViolation
    // above, there's no ambiguity to resolve here, so the source is always ValidationErrorSourceBody.
    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex,
            final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest request) {

        final var validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> buildInvalidAttributeMessageBody(
                        fieldError.getField(),
                        Objects.requireNonNullElse(fieldError.getDefaultMessage(), "validation failed"),
                        fieldError.getRejectedValue()))
                .toList();
        final var exception = new EntrypointException(validationErrors);

        return logExceptionAndBuild(exception);
    }

    // Handles Spring type-conversion failures on method parameters — fires BEFORE Bean Validation even runs
    // (e.g. "abc" passed where a ZoneId/Instant/int was expected on a @PathVariable, @RequestParam or
    // @RequestHeader). No ConstraintViolation/payload exists here, so the origin is resolved by inspecting
    // the parameter's own Spring annotation via exception.getParameter() instead.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleInvalidRequestParameters(final MethodArgumentTypeMismatchException exception) {
        final var invalidValue =
                Optional.ofNullable(exception.getValue()).map(Object::toString).orElse(null);
        final var detail =
                resolveFormatHint(exception.getRequiredType(), "invalid value for parameter: " + exception.getName());
        final var validationError = ValidationError.builder()
                .code(WebSupportExceptionCode.INVALID_VALUES)
                .detail(detail)
                .source(resolveSource(exception, invalidValue))
                .build();
        return logExceptionAndBuild(new EntrypointException(Collections.singletonList(validationError)));
    }

    // Handles malformed JSON in the request body — either a field that failed to parse into its target type
    // (InvalidFormatException, e.g. bad ZoneId/Instant string) or JSON the parser couldn't read at all. Always
    // originates from the body — same as handleMethodArgumentNotValid, source is always ValidationErrorSourceBody.
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

            final var invalidValue = Optional.ofNullable(invalidFormatException.getValue())
                    .map(Object::toString)
                    .orElse(null);
            final var detail = resolveFormatHint(targetType, "invalid value for type: " + targetType.getSimpleName());
            final var validationError = ValidationError.builder()
                    .code(WebSupportExceptionCode.INVALID_VALUES)
                    .detail(detail)
                    .source(new ValidationErrorSourceBody("$." + fieldName, invalidValue))
                    .build();
            return logExceptionAndBuild(new EntrypointException(Collections.singletonList(validationError), ex));
        }

        // For other HttpMessageNotReadableException cases, use default handling
        final var validationError = ValidationError.builder()
                .code(WebSupportExceptionCode.INVALID_VALUES)
                .detail("Failed to read request: " + ex.getMessage())
                .build();

        return logExceptionAndBuild(new EntrypointException(Collections.singletonList(validationError), ex));
    }

    private ResponseEntity<Object> logExceptionAndBuild(final CaimanException exception) {
        try {
            exception.executeLogging();

            final ProblemDetailResponseDto errorDto = caimanExceptionMapper.toProblemDetailResponse(exception);
            return ResponseEntity.status(exception.getHttpStatusCode()).body(errorDto);
        } finally {
            MDC.clear();
        }
    }

    private static ValidationError buildInvalidAttributeMessageBody(
            final String attributeName, final String errMotive, final Object attributeValue) {
        final var invalidValue =
                Optional.ofNullable(attributeValue).map(Object::toString).orElse(null);
        final var body = "$." + attributeName;
        final var source = new ValidationErrorSourceBody(body, invalidValue);

        if (errMotive.contains(Instant.class.getName())) {
            return ValidationError.builder()
                    .code(WebSupportExceptionCode.INVALID_VALUES)
                    .detail("date-time must be in the following format: %s (example: %s)"
                            .formatted(Constants.DATE_TIME_FORMAT, OpenApiConstants.Examples.DATE_TIME))
                    .source(source)
                    .build();
        }

        return ValidationError.builder()
                .code(WebSupportExceptionCode.INVALID_VALUES)
                .detail(errMotive)
                .source(source)
                .build();
    }

    private static String resolveFormatHint(final Class<?> clazz, final String fallback) {
        if (Objects.equals(clazz, ZoneId.class)) {
            return "timezone must be a valid tz database identifier (e.g., America/Sao_Paulo, UTC, Europe/London)";
        }
        if (Objects.equals(clazz, Instant.class)) {
            return "date-time must be in the following format: %s (example: %s)"
                    .formatted(Constants.DATE_TIME_FORMAT, OpenApiConstants.Examples.DATE_TIME);
        }
        if (Objects.equals(clazz, LocalDate.class)) {
            return "date must be in the following format: %s (example: %s)"
                    .formatted(Constants.DATE_FORMAT, OpenApiConstants.Examples.DATE);
        }
        if (Objects.equals(clazz, LocalTime.class)) {
            return "time must be in the following format: %s (example: %s)"
                    .formatted(Constants.TIME_FORMAT, OpenApiConstants.Examples.TIME);
        }
        return fallback;
    }

    private static ValidationErrorSource resolveSource(
            final MethodArgumentTypeMismatchException exception, final String invalidValue) {
        final var parameter = exception.getParameter();

        if (parameter.hasParameterAnnotation(PathVariable.class)) {
            return new ValidationErrorSourcePathParameter(exception.getName(), invalidValue);
        }

        if (parameter.hasParameterAnnotation(RequestHeader.class)) {
            return new ValidationErrorSourceHeader(exception.getName(), invalidValue);
        }

        if (parameter.hasParameterAnnotation(RequestParam.class)) {
            return new ValidationErrorSourceParameter(exception.getName(), invalidValue);
        }

        final var presentAnnotations = Arrays.stream(parameter.getParameterAnnotations())
                .map(annotation -> annotation.annotationType().getSimpleName())
                .collect(Collectors.joining(", "));

        log.error(
                LogField.Placeholders.TWO.getPlaceholder(),
                StructuredArguments.kv(
                        LogField.MSG.label(),
                        "Method parameter without a recognized origin annotation (@PathVariable/@RequestHeader/@RequestParam) — check that the parameter is properly annotated"),
                StructuredArguments.kv(
                        LogField.EXCEPTION_MESSAGE.label(),
                        "Annotations present: [%s]. Exception: %s".formatted(presentAnnotations, exception)));

        return new ValidationErrorSourceGeneric(exception.getName(), invalidValue);
    }

    private static ValidationErrorSource resolveSource(final ConstraintViolation<?> violation, final String paramName) {
        final Set<Class<? extends Payload>> payloads =
                violation.getConstraintDescriptor().getPayload();
        final var invalidValue = Optional.ofNullable(violation.getInvalidValue())
                .map(Object::toString)
                .orElse(null);

        if (payloads.contains(BodyParam.class)) {
            return new ValidationErrorSourceBody(paramName, invalidValue);
        }
        if (payloads.contains(HeaderParam.class)) {
            return new ValidationErrorSourceHeader(paramName, invalidValue);
        }
        if (payloads.contains(PathParam.class)) {
            return new ValidationErrorSourcePathParameter(paramName, invalidValue);
        }
        if (payloads.contains(QueryParam.class)) {
            return new ValidationErrorSourceParameter(paramName, invalidValue);
        }

        log.error(
                LogField.Placeholders.TWO.getPlaceholder(),
                StructuredArguments.kv(
                        LogField.MSG.label(),
                        "Constraint violation without a recognized origin payload (BodyParam/HeaderParam/PathParam/QueryParam) — check that the constraint annotation used is one of the composed ones"),
                StructuredArguments.kv(LogField.EXCEPTION_MESSAGE.label(), violation.toString()));

        return new ValidationErrorSourceGeneric(paramName, invalidValue);
    }
}
