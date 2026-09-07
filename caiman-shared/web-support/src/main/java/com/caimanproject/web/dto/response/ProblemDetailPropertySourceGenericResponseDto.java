package com.caimanproject.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProblemDetailPropertySourceGenericResponseDto extends ProblemDetailPropertySourceResponseDto {
    String field;

    @Builder
    public ProblemDetailPropertySourceGenericResponseDto(final String invalidValue, final String field) {
        super(invalidValue);
        this.field = field;
    }
}
