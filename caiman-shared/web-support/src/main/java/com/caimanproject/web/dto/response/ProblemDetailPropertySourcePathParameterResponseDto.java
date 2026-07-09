package com.caimanproject.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProblemDetailPropertySourcePathParameterResponseDto extends ProblemDetailPropertySourceResponseDto {
    String pathParameter;

    @Builder
    public ProblemDetailPropertySourcePathParameterResponseDto(final String invalidValue, final String pathParameter) {
        super(invalidValue);
        this.pathParameter = pathParameter;
    }
}
