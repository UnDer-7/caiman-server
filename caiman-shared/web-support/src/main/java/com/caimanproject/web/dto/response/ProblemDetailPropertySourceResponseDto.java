package com.caimanproject.web.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class ProblemDetailPropertySourceResponseDto {
    private final String invalidValue;

    protected ProblemDetailPropertySourceResponseDto(final String invalidValue) {
        this.invalidValue = invalidValue;
    }
}
