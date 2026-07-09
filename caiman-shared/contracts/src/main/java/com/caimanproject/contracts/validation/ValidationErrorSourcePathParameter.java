package com.caimanproject.contracts.validation;

import lombok.Builder;

@Builder
public record ValidationErrorSourcePathParameter(
    String pathParameter,
    String invalidValue
) implements ValidationErrorSource {

}
