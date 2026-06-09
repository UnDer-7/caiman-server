package com.caimanproject.contracts.util;

import com.caimanproject.contracts.exception.CaimanException;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public final class DomainValidation {

    private  DomainValidation() {
        throw new UnsupportedOperationException();
    }

    public static <T> T validateOrThrows(final T value, final String valueName, Function<String, CaimanException> exceptionFunc) {
        return switch (value) {
            case null -> throw exceptionFunc.apply("filed '" + valueName + "' cannot be null");
            case String str when str.isBlank() -> throw exceptionFunc.apply("filed '" + valueName + "' cannot be blank");
            case Collection<?> list when list.isEmpty() -> throw exceptionFunc.apply("filed '" + valueName + "' cannot be empty");
            case Map<?, ?> map when map.isEmpty() -> throw exceptionFunc.apply("filed '" + valueName + "' cannot be empty");
            default -> value;
        };
    }
}
