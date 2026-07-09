package com.caimanproject.contracts.validation;

import lombok.Builder;

@Builder
public record ValidationErrorSourceParameter(
    String queryParam,
    String invalidValue
) implements ValidationErrorSource {

}
