package com.caimanproject.billing.entrypoint.payload.response;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AuditResponseDto(
    @NotNull
    Instant createdAt,

    @NotNull
    Instant updatedAt) {

}
