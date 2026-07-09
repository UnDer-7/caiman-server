package com.caimanproject.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProblemDetailPropertySourceBodyResponseDto extends ProblemDetailPropertySourceResponseDto {
    String body;

    @Builder
    public ProblemDetailPropertySourceBodyResponseDto(final String invalidValue,  final String body) {
        super(invalidValue);
        this.body = body;
    }

}
