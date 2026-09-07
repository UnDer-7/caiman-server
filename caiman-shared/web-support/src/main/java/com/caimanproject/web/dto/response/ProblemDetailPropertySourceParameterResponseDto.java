package com.caimanproject.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProblemDetailPropertySourceParameterResponseDto extends ProblemDetailPropertySourceResponseDto {
    String queryParameter;

    @Builder
    public ProblemDetailPropertySourceParameterResponseDto(final String invalidValue, final String queryParameter) {
        super(invalidValue);
        this.queryParameter = queryParameter;
    }
}
