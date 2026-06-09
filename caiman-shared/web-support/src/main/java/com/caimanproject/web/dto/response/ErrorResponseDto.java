package com.caimanproject.web.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ErrorResponseDto(
    String code,
    Instant timestamp,
    String message,
    String detail,
    int httpStatusCode
) {

}
