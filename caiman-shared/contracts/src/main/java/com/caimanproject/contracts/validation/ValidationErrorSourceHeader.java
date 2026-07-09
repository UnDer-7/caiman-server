package com.caimanproject.contracts.validation;

import lombok.Builder;

@Builder
public record ValidationErrorSourceHeader(String header, String invalidValue) implements ValidationErrorSource {}
