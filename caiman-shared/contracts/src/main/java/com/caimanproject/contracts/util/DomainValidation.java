package com.caimanproject.contracts.util;

import com.caimanproject.contracts.exception.ExceptionCode;
import com.caimanproject.contracts.validation.ValidationError;
import com.caimanproject.contracts.validation.ValidationErrorSourceBody;
import com.caimanproject.contracts.validation.ValidationResult;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class DomainValidation {

    private DomainValidation() {
        throw new UnsupportedOperationException();
    }

    public static Optional<ValidationError> validate(
        final Object value, final String fieldPath, final ExceptionCode code) {
        return switch (value) {
            case null -> build(code, "field cannot be null", fieldPath, null);
            case String str when str.isBlank() -> build(code, "field cannot be blank", fieldPath, str);
            case Collection<?> list when list.isEmpty() -> build(code, "field cannot be empty", fieldPath, list);
            case Map<?, ?> map when map.isEmpty() -> build(code, "field cannot be empty", fieldPath, map);
            default -> Optional.empty();
        };
    }

    public static ValidationResult validateAll(final List<Optional<ValidationError>> errors) {
        final var validErrors =  errors.stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

        return ValidationResult.of(validErrors);
    }

    private static Optional<ValidationError> build(final ExceptionCode code, String detail, String fieldPath, final Object value) {
        return Optional.of(ValidationError.builder()
            .code(code)
            .detail(detail)
            .source(value == null ? new ValidationErrorSourceBody(fieldPath, null) : new ValidationErrorSourceBody(fieldPath, String.valueOf(value)))
            .build());
    }

}
