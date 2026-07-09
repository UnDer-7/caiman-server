package com.caimanproject.contracts.validation;

public record ValidationErrorSourceGeneric(
    String fieldName,
    String invalidValue
) implements ValidationErrorSource {

}
