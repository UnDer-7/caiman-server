package com.caimanproject.billing.entrypoint.payload.response;

import java.time.Instant;

public record AuditResponseDto(Instant createdAt, Instant updatedAt) {}
