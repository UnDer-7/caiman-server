package com.caimanproject.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProblemDetailPropertySourceHeaderResponseDto extends ProblemDetailPropertySourceResponseDto {
    String header;

    @Builder
    public ProblemDetailPropertySourceHeaderResponseDto(final String invalidValue, final String header) {
        super(invalidValue);
        this.header = header;
    }
}
