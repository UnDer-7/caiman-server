package com.caimanproject.contracts.validation;

import lombok.Builder;

@Builder
public record ValidationErrorSourceBody(
    String body,
    String invalidValue
) implements ValidationErrorSource {

}
