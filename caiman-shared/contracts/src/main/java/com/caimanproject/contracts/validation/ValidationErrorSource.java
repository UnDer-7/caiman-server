package com.caimanproject.contracts.validation;

public sealed interface ValidationErrorSource
        permits ValidationErrorSourceBody,
                ValidationErrorSourceParameter,
                ValidationErrorSourceHeader,
                ValidationErrorSourcePathParameter,
                ValidationErrorSourceGeneric {

    String invalidValue();
}
