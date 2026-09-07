package com.caimanproject.web.dto.response;

import lombok.Builder;

@Builder
public record ProblemDetailPropertyErrorResponseDto(
        String code, String message, String detail, ProblemDetailPropertySourceResponseDto source) {}
