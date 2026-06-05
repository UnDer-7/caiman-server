package com.caimanproject.contracts.util;

import java.util.Collection;
import java.util.Map;

public final class DomainValidation {

    private  DomainValidation() {
        throw new UnsupportedOperationException();
    }

    public static <T> T validateAndReturn(final T value, final String valueName) {
        return switch (value) {
            case null -> throw new IllegalArgumentException("filed '" + valueName + "' cannot be null");
            case String str when str.isBlank() -> throw new IllegalArgumentException("filed '" + valueName + "' cannot be blank");
            case Collection<?> list when list.isEmpty() -> throw new IllegalArgumentException("filed '" + valueName + "' cannot be empty");
            case Map<?, ?> map when map.isEmpty() -> throw new IllegalArgumentException("filed '" + valueName + "' cannot be empty");
            default -> value;
        };
    }
}
