package com.caimanproject.contracts.validation;

import com.caimanproject.contracts.exception.CaimanException;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public record ValidationResult(List<ValidationError> errors) {

    public ValidationResult {
        errors = errors == null ? Collections.emptyList() : List.copyOf(errors);
    }

    public static ValidationResult of(final ValidationError errors) {
        return of(Collections.singletonList(errors));
    }

    public static ValidationResult of(final List<ValidationError> errors) {
        return new ValidationResult(errors);
    }

    public static ValidationResult valid() {
        return new ValidationResult(Collections.emptyList());
    }

    public ValidationResult merge(final ValidationResult other) {
        return new ValidationResult(Stream.concat(errors.stream(), other.errors().stream()).toList());
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public boolean isInvalid() {
        return !isValid();
    }

    public void throwIfInvalid(final Function<List<ValidationError>, CaimanException> exceptionSupplier) {
        if (isInvalid()) {
            throw exceptionSupplier.apply(errors);
        }
    }

}
